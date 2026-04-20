package com.example.heatcalculate.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 热量识别结果
 */
@Schema(description = "食物热量识别结果")
public class CalorieResult {

    @Schema(description = "会话标识，用于后续纠正操作")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sessionId;

    @Schema(description = "识别的食物列表")
    private List<FoodItem> foods;

    @Schema(description = "总热量区间")
    private CalorieRange totalCalories;

    @Schema(description = "免责说明", example = "热量数据为估算值，实际值因食材和烹饪方式而异")
    private String disclaimer;

    public CalorieResult() {
    }

    public CalorieResult(List<FoodItem> foods, CalorieRange totalCalories, String disclaimer) {
        this.foods = foods;
        this.totalCalories = totalCalories;
        this.disclaimer = disclaimer;
    }

    public static CalorieResultBuilder builder() {
        return new CalorieResultBuilder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<FoodItem> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodItem> foods) {
        this.foods = foods;
    }

    public CalorieRange getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(CalorieRange totalCalories) {
        this.totalCalories = totalCalories;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public static class CalorieResultBuilder {
        private String sessionId;
        private List<FoodItem> foods;
        private CalorieRange totalCalories;
        private String disclaimer;

        public CalorieResultBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public CalorieResultBuilder foods(List<FoodItem> foods) {
            this.foods = foods;
            return this;
        }

        public CalorieResultBuilder totalCalories(CalorieRange totalCalories) {
            this.totalCalories = totalCalories;
            return this;
        }

        public CalorieResultBuilder disclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
            return this;
        }

        public CalorieResult build() {
            CalorieResult result = new CalorieResult(foods, totalCalories, disclaimer);
            result.setSessionId(sessionId);
            return result;
        }
    }
}
