package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.exception.SessionExpiredException;
import com.example.heatcalculate.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 结果纠正服务
 * 将用户的纠正信息追加到会话 chatMemory，调用模型重新推理
 */
@Service
public class CorrectionService {

    private static final Logger log = LoggerFactory.getLogger(CorrectionService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        "}";

    private final SessionStore sessionStore;
    private final ChatLanguageModel chatModel;

    public CorrectionService(SessionStore sessionStore, ChatLanguageModel chatModel) {
        this.sessionStore = sessionStore;
        this.chatModel = chatModel;
    }

    /**
     * 纠正分析结果
     *
     * @param sessionId      会话ID
     * @param corrections    纠正项列表
     * @param additionalNote 补充备注
     * @return 重新推理后的结果（含 sessionId）
     */
    public CalorieResult correct(String sessionId, List<CorrectionRequest.CorrectionItem> corrections, String additionalNote) {
        log.info("开始纠正，sessionId: {}", sessionId);

        // 2.3.1 获取会话
        Optional<AnalysisSession> optSession = sessionStore.get(sessionId);
        if (optSession.isEmpty()) {
            throw new SessionExpiredException("会话不存在或已过期，请重新上传图片");
        }
        AnalysisSession session = optSession.get();

        // 2.3.2 构建纠正 UserMessage
        String correctionMessage = buildCorrectionMessage(corrections, additionalNote);
        log.info("纠正消息: {}", correctionMessage);

        // 2.3.3 重建消息列表，追加纠正消息，调用模型
        try {
            List<ChatMessage> messages = rebuildMessages(session);
            messages.add(UserMessage.from(correctionMessage));

            Response<AiMessage> response = chatModel.generate(messages);
            String aiResponse = response.content().text();
            log.info("纠正后模型返回: {}", aiResponse);

            // 2.3.4 解析响应，更新会话
            CalorieResult result = parseResponse(aiResponse);
            result.setSessionId(sessionId);

            // 追加到 chatHistory
            session.getChatHistory().add(Map.of("role", "user", "content", correctionMessage));
            session.getChatHistory().add(Map.of("role", "assistant", "content", aiResponse));
            session.setLastResult(result);

            log.info("纠正完成，sessionId: {}", sessionId);
            return result;
        } catch (SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("纠正过程模型调用失败: {}", e.getMessage(), e);
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 构建纠正消息
     */
    String buildCorrectionMessage(List<CorrectionRequest.CorrectionItem> corrections, String additionalNote) {
        StringBuilder sb = new StringBuilder("用户纠正：\n");

        if (corrections != null && !corrections.isEmpty()) {
            for (CorrectionRequest.CorrectionItem item : corrections) {
                sb.append(String.format("- 第%d项", item.getIndex() + 1));
                if (item.getName() != null && !item.getName().isEmpty()) {
                    sb.append(String.format("食物名称改为「%s」", item.getName()));
                }
                if (item.getWeight() != null && !item.getWeight().isEmpty()) {
                    sb.append(String.format("，重量改为%s", item.getWeight()));
                }
                sb.append("\n");
            }
        }

        if (additionalNote != null && !additionalNote.trim().isEmpty()) {
            sb.append("补充说明：").append(additionalNote.trim()).append("\n");
        }

        sb.append("\n请基于以上修正重新计算所有食物的热量。输出格式与之前相同（JSON结构）。");
        return sb.toString();
    }

    /**
     * 从会话历史重建消息列表
     */
    private List<ChatMessage> rebuildMessages(AnalysisSession session) {
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

        return messages;
    }

    /**
     * 解析 AI 响应
     */
    private CalorieResult parseResponse(String response) {
        try {
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            // 去掉可能的 QUESTION 行
            int questionIdx = json.indexOf("QUESTION:");
            if (questionIdx > 0) {
                json = json.substring(0, questionIdx).trim();
            }
            return objectMapper.readValue(json.trim(), CalorieResult.class);
        } catch (Exception e) {
            log.error("解析纠正结果 AI 响应失败: {}", response, e);
            CalorieResult result = new CalorieResult();
            result.setFoods(new ArrayList<>());
            CalorieRange range = new CalorieRange();
            range.setLow(0);
            range.setMid(0);
            range.setHigh(0);
            result.setTotalCalories(range);
            result.setDisclaimer("解析结果失败，请重试");
            return result;
        }
    }
}
