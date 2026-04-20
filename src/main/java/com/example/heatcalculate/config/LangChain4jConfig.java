package com.example.heatcalculate.config;

import dev.langchain4j.model.dashscope.QwenChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.dash-scope.api-key}")
    private String apiKey;

    @Value("${langchain4j.dash-scope.model-name:qwen-vl-max}")
    private String modelName;

    /**
     * 配置通义千问-VL 模型
     */
    @Bean
    public QwenChatModel qwenChatModel() {
        return QwenChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .build();
    }
}
