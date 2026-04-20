package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.model.CalorieRange;
import com.example.heatcalculate.model.CalorieResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 热量计算服务
 * 使用 LangChain4j UserMessage + ImageContent 正确传递图片，修复 Base64 文本传图问题
 */
@Service
public class CalorieService {

    private static final Logger log = LoggerFactory.getLogger(CalorieService.class);
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

    private final ImageValidatorService imageValidatorService;
    private final ChatLanguageModel chatModel;

    public CalorieService(ImageValidatorService imageValidatorService, ChatLanguageModel chatModel) {
        this.imageValidatorService = imageValidatorService;
        this.chatModel = chatModel;
    }

    /**
     * 分析食物图片并返回热量估算
     */
    public CalorieResult analyzeFood(MultipartFile image, String note) {
        log.info("开始校验图片: {}, 大小: {} bytes", image.getOriginalFilename(), image.getSize());
        imageValidatorService.validate(image);
        log.info("图片校验通过");

        String base64Data;
        String mimeType;
        try {
            base64Data = Base64.getEncoder().encodeToString(image.getBytes());
            mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            log.info("图片Base64编码完成，长度: {} 字符", base64Data.length());
        } catch (IOException e) {
            log.error("图片编码失败", e);
            throw new ModelServiceException("图片处理失败", e);
        }

        try {
            log.info("开始调用AI服务分析图片...");

            String userText = note != null && !note.isEmpty()
                ? "用户备注：" + note + "\n\n请分析这张食物图片，识别其中的食物并估算热量。"
                : "请分析这张食物图片，识别其中的食物并估算热量。";

            // 使用 LangChain4j ImageContent 正确传递图片（而非把 Base64 塞进文本）
            UserMessage userMessage = UserMessage.from(
                ImageContent.from(base64Data, mimeType),
                TextContent.from(SYSTEM_PROMPT + "\n\n" + userText)
            );

            Response<AiMessage> response = chatModel.generate(List.of(userMessage));
            String aiResponse = response.content().text();
            log.info("AI服务返回结果: {}", aiResponse);

            return parseResponse(aiResponse);
        } catch (Exception e) {
            log.error("模型调用失败: {}", e.getMessage(), e);
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试", e);
        }
    }

    CalorieResult parseResponse(String response) {
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
            return objectMapper.readValue(json.trim(), CalorieResult.class);
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", response, e);
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
