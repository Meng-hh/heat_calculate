package com.example.heatcalculate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 食物项
 */
@Schema(description = "识别出的食物项")
public class FoodItem {

    @Schema(description = "食物名称", example = "红烧肉")
    private String name;

    @Schema(description = "估算重量范围", example = "80-120g")
    private String estimatedWeight;

    @Schema(description = "热量区间")
    private CalorieRange calories;

    public FoodItem() {
    }

    public FoodItem(String name, String estimatedWeight, CalorieRange calories) {
        this.name = name;
        this.estimatedWeight = estimatedWeight;
        this.calories = calories;
    }

    public static FoodItemBuilder builder() {
        return new FoodItemBuilder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEstimatedWeight() {
        return estimatedWeight;
    }

    public void setEstimatedWeight(String estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }

    public CalorieRange getCalories() {
        return calories;
    }

    public void setCalories(CalorieRange calories) {
        this.calories = calories;
    }

    public static class FoodItemBuilder {
        private String name;
        private String estimatedWeight;
        private CalorieRange calories;

        public FoodItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FoodItemBuilder estimatedWeight(String estimatedWeight) {
            this.estimatedWeight = estimatedWeight;
            return this;
        }

        public FoodItemBuilder calories(CalorieRange calories) {
            this.calories = calories;
            return this;
        }

        public FoodItem build() {
            return new FoodItem(name, estimatedWeight, calories);
        }
    }
}
