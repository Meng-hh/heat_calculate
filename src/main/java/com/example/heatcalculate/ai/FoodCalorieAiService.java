package com.example.heatcalculate.ai;

import com.example.heatcalculate.model.CalorieResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 食物热量 AI 服务接口
 * 使用 LangChain4j @AiServices 实现结构化输出
 */
public interface FoodCalorieAiService {

    @SystemMessage({
        "你是一个专业的食物热量估算助手。请根据用户上传的食物图片，识别图中所有食物，估算份量并计算热量区间。",
        "",
        "## 标准餐具尺寸参照（用于估算份量）：",
        "- 饭碗：直径约12cm，盛满米饭约200g",
        "- 餐盘：直径约24cm",
        "- 汤碗：直径约16cm，容量约500ml",
        "- 筷子：长度约24cm（可作为图片中的天然比例尺）",
        "",
        "## 估算规则：",
        "1. 根据餐具参照物估算食物体积和重量",
        "2. 考虑食物的烹饪方式（油炸、清蒸、红烧等会影响热量）",
        "3. 返回低、中、高三个热量估值，体现估算的不确定性",
        "4. 如果图片中没有可识别的食物，返回空列表",
        "",
        "## 输出格式要求：",
        "必须返回以下JSON结构：",
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
        "}"
    })
    @UserMessage({
        "请分析这张食物图片，识别其中的食物并估算热量。",
        "{{note}}",
        "",
        "图片数据：{{imageBase64}}"
    })
    CalorieResult analyzeFoodImage(@V("imageBase64") String imageBase64, @V("note") String note);
}
