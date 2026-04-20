package com.example.heatcalculate.service;

import com.example.heatcalculate.ai.FoodCalorieAiService;
import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.model.CalorieResult;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * 热量计算服务
 * 编排：图片校验 → Base64编码 → 调用AI服务 → 返回结果
 */
@Service
public class CalorieService {

    private static final Logger log = LoggerFactory.getLogger(CalorieService.class);

    private final ImageValidatorService imageValidatorService;
    private final QwenChatModel qwenChatModel;

    public CalorieService(ImageValidatorService imageValidatorService, QwenChatModel qwenChatModel) {
        this.imageValidatorService = imageValidatorService;
        this.qwenChatModel = qwenChatModel;
    }

    /**
     * 分析食物图片并返回热量估算
     *
     * @param image 图片文件
     * @param note  可选备注
     * @return 热量识别结果
     */
    public CalorieResult analyzeFood(MultipartFile image, String note) {
        // 1. 图片校验
        log.info("开始校验图片: {}, 大小: {} bytes", image.getOriginalFilename(), image.getSize());
        imageValidatorService.validate(image);
        log.info("图片校验通过");

        // 2. Base64 编码
        String base64Image;
        try {
            base64Image = encodeToBase64(image);
            log.info("图片Base64编码完成，长度: {} 字符", base64Image.length());
        } catch (IOException e) {
            log.error("图片编码失败", e);
            throw new ModelServiceException("图片处理失败", e);
        }

        // 3. 创建 AI 服务代理
        FoodCalorieAiService aiService = AiServices.create(FoodCalorieAiService.class, qwenChatModel);

        // 4. 调用 AI 服务
        try {
            log.info("开始调用AI服务分析图片...");
            CalorieResult result = aiService.analyzeFoodImage(base64Image, note != null ? note : "");
            log.info("AI服务返回结果: 识别到 {} 种食物", result.getFoods() != null ? result.getFoods().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("模型调用失败: {}", e.getMessage(), e);
            throw new ModelServiceException("模型服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 将图片文件编码为 Base64
     */
    private String encodeToBase64(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        // 添加 data URI 前缀，便于模型识别
        String mimeType = file.getContentType();
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return "data:" + mimeType + ";base64," + base64;
    }
}
