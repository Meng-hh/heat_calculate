package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.model.CalorieResult;
import com.example.heatcalculate.model.CalorieRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 热量计算服务
 * 直接调用 DashScope API
 */
@Service
public class CalorieService {

    private static final Logger log = LoggerFactory.getLogger(CalorieService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final ImageValidatorService imageValidatorService;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String modelName;

    public CalorieService(ImageValidatorService imageValidatorService,
                          @Value("${langchain4j.dashscope.api-key}") String apiKey,
                          @Value("${langchain4j.dashscope.model-name:qwen-vl-max}") String modelName) {
        this.imageValidatorService = imageValidatorService;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.restTemplate = new RestTemplate();
    }

    public CalorieResult analyzeFood(MultipartFile image, String note) {
        log.info("开始校验图片: {}, 大小: {} bytes", image.getOriginalFilename(), image.getSize());
        imageValidatorService.validate(image);
        log.info("图片校验通过");

        String base64Image;
        try {
            base64Image = encodeToBase64(image);
            log.info("图片Base64编码完成，长度: {} 字符", base64Image.length());
        } catch (IOException e) {
            log.error("图片编码失败", e);
            throw new ModelServiceException("图片处理失败", e);
        }

        try {
            log.info("开始调用AI服务分析图片...");
            
            String prompt = buildPrompt(note);
            
            // 构建请求体 - 使用 data URL 格式
            String imageUrl = "data:image/jpeg;base64," + base64Image;
            Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "input", Map.of(
                    "messages", List.of(
                        Map.of(
                            "role", "user",
                            "content", List.of(
                                Map.of("image", imageUrl),
                                Map.of("text", prompt)
                            )
                        )
                    )
                ),
                "parameters", Map.of(
                    "result_format", "message"
                )
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(DASHSCOPE_API_URL, entity, Map.class);
            
            Map<String, Object> body = response.getBody();
            Map<String, Object> output = (Map<String, Object>) body.get("output");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            List<Map<String, Object>> content = (List<Map<String, Object>>) message.get("content");
            String aiResponse = (String) content.get(0).get("text");
            
            log.info("AI服务返回结果: {}", aiResponse);
            return parseResponse(aiResponse);
            
        } catch (Exception e) {
            log.error("模型调用失败: {}", e.getMessage(), e);
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String note) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的食物热量估算助手。请根据用户上传的食物图片，识别图中所有食物，估算份量并计算热量区间。\n\n");
        
        if (note != null && !note.isEmpty()) {
            prompt.append("用户备注：").append(note).append("\n\n");
        }
        
        prompt.append("## 标准餐具尺寸参照（用于估算份量）：\n");
        prompt.append("- 饭碗：直径约12cm，盛满米饭约200g\n");
        prompt.append("- 餐盘：直径约24cm\n");
        prompt.append("- 汤碗：直径约16cm，容量约500ml\n");
        prompt.append("- 筷子：长度约24cm（可作为图片中的天然比例尺）\n\n");
        
        prompt.append("## 估算规则：\n");
        prompt.append("1. 根据餐具参照物估算食物体积和重量\n");
        prompt.append("2. 考虑食物的烹饪方式（油炸、清蒸、红烧等会影响热量）\n");
        prompt.append("3. 返回低、中、高三个热量估值，体现估算的不确定性\n");
        prompt.append("4. 如果图片中没有可识别的食物，返回空列表\n\n");
        
        prompt.append("## 输出格式要求：\n");
        prompt.append("必须返回以下JSON结构（不要添加markdown代码块标记）：\n");
        prompt.append("{\n");
        prompt.append("  \"foods\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"食物名称\",\n");
        prompt.append("      \"estimatedWeight\": \"重量范围，如80-120g\",\n");
        prompt.append("      \"calories\": {\n");
        prompt.append("        \"low\": 低估值（整数）,\n");
        prompt.append("        \"mid\": 中估值（整数）,\n");
        prompt.append("        \"high\": 高估值（整数）\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"totalCalories\": {\n");
        prompt.append("    \"low\": 总热量低估值,\n");
        prompt.append("    \"mid\": 总热量中估值,\n");
        prompt.append("    \"high\": 总热量高估值\n");
        prompt.append("  },\n");
        prompt.append("  \"disclaimer\": \"热量数据为估算值，实际值因食材和烹饪方式而异\"\n");
        prompt.append("}\n\n");
        prompt.append("请分析这张图片并返回JSON格式的结果。");
        
        return prompt.toString();
    }

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
            json = json.trim();
            
            return objectMapper.readValue(json, CalorieResult.class);
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

    private String encodeToBase64(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }
}
