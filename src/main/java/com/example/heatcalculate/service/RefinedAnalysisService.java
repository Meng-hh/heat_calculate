package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 精细分析服务
 * 支持多轮对话，通过追问缩小热量估算区间
 */
@Service
public class RefinedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RefinedAnalysisService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_ROUNDS = 5;
    private static final int CALORIE_THRESHOLD = 200;

    private static final String SYSTEM_PROMPT =
        "你是一个专业的食物热量估算助手。请根据用户上传的食物图片，识别图中所有食物，估算份量并计算热量区间。\n\n" +
        "## 标准餐具尺寸参照（用于估算份量）：\n" +
        "- 饭碗：直径约12cm，盛满米饭约200g\n" +
        "- 餐盘：直径约24cm\n" +
        "- 汤碗：直径约16cm，容量约500ml\n" +
        "- 筷子：长度约24cm（可作为图片中的天然比例尺）\n\n" +
        "## 估算规则：\n" +
        "1. 根据餐具参照物估算食物体积和重量\n" +
        "2. 考虑食物的烹饪方式（油炸、清蒸、红烧等会影响热量）\n" +
        "3. 返回低、中、高三个热量估值，体现估算的不确定性\n" +
        "4. 如果图片中没有可识别的食物，返回空列表\n\n" +
        "## 输出格式要求：\n" +
        "必须返回以下JSON结构（不要添加markdown代码块标记）：\n" +
        "{\n" +
        "  \"foods\": [\n" +
        "    {\n" +
        "      \"name\": \"食物名称\",\n" +
        "      \"estimatedWeight\": \"重量范围，如80-120g\",\n" +
        "      \"calories\": {\n" +
        "        \"low\": 低估值（整数）,\n" +
        "        \"mid\": 中估值（整数）,\n" +
        "        \"high\": 高估值（整数）\n" +
        "      }\n" +
        "    }\n" +
        "  ],\n" +
        "  \"totalCalories\": {\n" +
        "    \"low\": 总热量低估值,\n" +
        "    \"mid\": 总热量中估值,\n" +
        "    \"high\": 总热量高估值\n" +
        "  },\n" +
        "  \"disclaimer\": \"热量数据为估算值，实际值因食材和烹饪方式而异\"\n" +
        "}\n\n" +
        "## 追问模式：\n" +
        "如果你对食物的种类、份量或烹饪方式不确定，导致热量区间过宽（high - low > 200kcal），" +
        "请在JSON之后另起一行，以 \"QUESTION:\" 开头提出一个针对热量影响最大的不确定因素的问题。\n" +
        "例如：QUESTION:这碗面是素面还是加了肉？\n" +
        "如果你已经足够确定，不需要追问，则不要输出 QUESTION 行。";

    private static final String CONTINUE_PROMPT_TEMPLATE =
        "用户回答了你的问题：\"%s\"\n\n" +
        "请根据这个新信息，重新估算食物热量。输出格式与之前相同（JSON + 可选的 QUESTION 行）。" +
        "如果你仍然不确定，可以继续追问（以 QUESTION: 开头）。如果已经足够确定，只返回最终JSON结果。";

    private final ImageValidatorService imageValidatorService;
    private final ChatLanguageModel chatModel;
    private final SessionStore sessionStore;

    public RefinedAnalysisService(ImageValidatorService imageValidatorService,
                                  ChatLanguageModel chatModel,
                                  SessionStore sessionStore) {
        this.imageValidatorService = imageValidatorService;
        this.chatModel = chatModel;
        this.sessionStore = sessionStore;
    }

    /**
     * 开始精细分析
     */
    public RefinedAnalysisResponse startRefinedAnalysis(MultipartFile image, String note) {
        log.info("开始精细分析: {}, 大小: {} bytes", image.getOriginalFilename(), image.getSize());
        imageValidatorService.validate(image);

        String base64Data;
        String mimeType;
        try {
            base64Data = Base64.getEncoder().encodeToString(image.getBytes());
            mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
        } catch (IOException e) {
            throw new ModelServiceException("图片处理失败", e);
        }

        String userText = note != null && !note.isEmpty()
            ? "用户备注：" + note + "\n\n请分析这张食物图片，识别其中的食物并估算热量。"
            : "请分析这张食物图片，识别其中的食物并估算热量。";

        try {
            // 构建首轮消息
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(SYSTEM_PROMPT));
            UserMessage userMessage = UserMessage.from(
                ImageContent.from(base64Data, mimeType),
                TextContent.from(userText)
            );
            messages.add(userMessage);

            // 调用模型
            Response<AiMessage> response = chatModel.generate(messages);
            String aiResponse = response.content().text();
            log.info("精细分析首轮返回: {}", aiResponse);

            // 解析响应
            ParsedResponse parsed = parseAiResponse(aiResponse);
            CalorieResult result = parsed.result;

            // 判断是否需要追问
            if (parsed.question != null && shouldAskClarification(result)) {
                // 创建会话
                String sessionId = UUID.randomUUID().toString();
                AnalysisSession session = new AnalysisSession(sessionId);
                session.setLastResult(result);
                session.setCurrentQuestion(parsed.question);
                session.setRoundCount(1);

                // 保存对话历史用于后续轮次
                List<Map<String, Object>> chatHistory = new ArrayList<>();
                chatHistory.add(Map.of("role", "user", "content", userText, "hasImage", true,
                    "base64Data", base64Data, "mimeType", mimeType));
                chatHistory.add(Map.of("role", "assistant", "content", aiResponse));
                session.setChatHistory(chatHistory);

                sessionStore.put(sessionId, session);
                log.info("精细分析需要追问，sessionId: {}, question: {}", sessionId, parsed.question);

                return RefinedAnalysisResponse.needInput(sessionId, parsed.question, result);
            } else {
                // 首轮就足够精确，创建会话用于后续纠正
                String sessionId = UUID.randomUUID().toString();
                AnalysisSession session = new AnalysisSession(sessionId);
                session.setLastResult(result);

                List<Map<String, Object>> chatHistory = new ArrayList<>();
                chatHistory.add(Map.of("role", "user", "content", userText, "hasImage", true,
                    "base64Data", base64Data, "mimeType", mimeType));
                chatHistory.add(Map.of("role", "assistant", "content", aiResponse));
                session.setChatHistory(chatHistory);

                sessionStore.put(sessionId, session);
                log.info("精细分析首轮即完成，无需追问，sessionId: {}", sessionId);
                return RefinedAnalysisResponse.complete(sessionId, result);
            }
        } catch (Exception e) {
            log.error("精细分析模型调用失败: {}", e.getMessage(), e);
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 继续精细分析（用户回答追问后）
     */
    public RefinedAnalysisResponse continueRefinedAnalysis(String sessionId, String answer) {
        log.info("继续精细分析，sessionId: {}, answer: {}", sessionId, answer);

        Optional<AnalysisSession> optSession = sessionStore.get(sessionId);
        if (optSession.isEmpty()) {
            return null; // 表示会话不存在或过期，Controller 处理为 404
        }

        AnalysisSession session = optSession.get();

        // 检查是否达到最大轮数
        if (session.getRoundCount() >= MAX_ROUNDS) {
            log.info("达到最大追问轮数 {}，返回最佳估算，会话保留用于纠正", MAX_ROUNDS);
            return RefinedAnalysisResponse.complete(sessionId, session.getLastResult());
        }

        try {
            // 重建消息列表用于 API 调用
            List<ChatMessage> messages = rebuildMessages(session, answer);

            // 调用模型
            Response<AiMessage> response = chatModel.generate(messages);
            String aiResponse = response.content().text();
            log.info("精细分析第 {} 轮返回: {}", session.getRoundCount() + 1, aiResponse);

            // 解析响应
            ParsedResponse parsed = parseAiResponse(aiResponse);
            CalorieResult result = parsed.result;

            // 更新会话
            session.setRoundCount(session.getRoundCount() + 1);
            session.setLastResult(result);

            // 追加历史
            session.getChatHistory().add(Map.of("role", "user", "content",
                String.format(CONTINUE_PROMPT_TEMPLATE, answer)));
            session.getChatHistory().add(Map.of("role", "assistant", "content", aiResponse));

            // 判断是否继续追问
            if (parsed.question != null && shouldAskClarification(result) && session.getRoundCount() < MAX_ROUNDS) {
                session.setCurrentQuestion(parsed.question);
                log.info("继续追问，round: {}, question: {}", session.getRoundCount(), parsed.question);
                return RefinedAnalysisResponse.needInput(sessionId, parsed.question, result);
            } else {
                // 完成，保留会话用于后续纠正
                session.setLastResult(result);
                log.info("精细分析完成，共 {} 轮，会话保留用于纠正", session.getRoundCount());
                return RefinedAnalysisResponse.complete(sessionId, result);
            }
        } catch (Exception e) {
            log.error("精细分析继续调用失败: {}", e.getMessage(), e);
            // 返回当前最佳结果，保留会话用于纠正
            if (session.getLastResult() != null) {
                return RefinedAnalysisResponse.complete(sessionId, session.getLastResult());
            }
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 判断是否需要追问（热量区间宽度 > 200kcal）
     */
    boolean shouldAskClarification(CalorieResult result) {
        if (result == null || result.getTotalCalories() == null) {
            return false;
        }
        CalorieRange range = result.getTotalCalories();
        if (range.getHigh() == null || range.getLow() == null) {
            return false;
        }
        return (range.getHigh() - range.getLow()) > CALORIE_THRESHOLD;
    }

    /**
     * 解析 AI 响应（JSON + 可选的 QUESTION 行）
     */
    private ParsedResponse parseAiResponse(String aiResponse) {
        ParsedResponse parsed = new ParsedResponse();

        String json = aiResponse.trim();
        String question = null;

        // 分离 QUESTION 行
        int questionIdx = json.indexOf("QUESTION:");
        if (questionIdx > 0) {
            question = json.substring(questionIdx + "QUESTION:".length()).trim();
            json = json.substring(0, questionIdx).trim();
        }

        // 清理 markdown 代码块标记
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }

        try {
            parsed.result = objectMapper.readValue(json.trim(), CalorieResult.class);
        } catch (Exception e) {
            log.error("解析精细分析 AI 响应失败: {}", aiResponse, e);
            CalorieResult fallback = new CalorieResult();
            fallback.setFoods(new ArrayList<>());
            CalorieRange range = new CalorieRange();
            range.setLow(0);
            range.setMid(0);
            range.setHigh(0);
            fallback.setTotalCalories(range);
            fallback.setDisclaimer("解析结果失败，请重试");
            parsed.result = fallback;
        }

        parsed.question = question;
        return parsed;
    }

    /**
     * 重建消息列表用于多轮对话
     */
    private List<ChatMessage> rebuildMessages(AnalysisSession session, String newAnswer) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));

        for (Map<String, Object> entry : session.getChatHistory()) {
            String role = (String) entry.get("role");
            String content = (String) entry.get("content");

            if ("user".equals(role)) {
                Boolean hasImage = (Boolean) entry.get("hasImage");
                if (Boolean.TRUE.equals(hasImage)) {
                    String base64Data = (String) entry.get("base64Data");
                    String mimeType = (String) entry.get("mimeType");
                    messages.add(UserMessage.from(
                        ImageContent.from(base64Data, mimeType),
                        TextContent.from(content)
                    ));
                } else {
                    messages.add(UserMessage.from(content));
                }
            } else if ("assistant".equals(role)) {
                messages.add(AiMessage.from(content));
            }
        }

        // 追加新的用户回答
        String continueText = String.format(CONTINUE_PROMPT_TEMPLATE, newAnswer);
        messages.add(UserMessage.from(continueText));

        return messages;
    }

    /**
     * 内部解析结果
     */
    private static class ParsedResponse {
        CalorieResult result;
        String question;
    }
}
