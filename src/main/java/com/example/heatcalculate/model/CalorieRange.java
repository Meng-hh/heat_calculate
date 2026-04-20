package com.example.heatcalculate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 热量区间
 */
@Schema(description = "热量区间（低/中/高）")
public class CalorieRange {

    @Schema(description = "低估值（千卡）", example = "280")
    private Integer low;

    @Schema(description = "中估值（千卡）", example = "380")
    private Integer mid;

    @Schema(description = "高估值（千卡）", example = "480")
    private Integer high;

    public CalorieRange() {
    }

    public CalorieRange(Integer low, Integer mid, Integer high) {
        this.low = low;
        this.mid = mid;
        this.high = high;
    }

    public static CalorieRangeBuilder builder() {
        return new CalorieRangeBuilder();
    }

    public Integer getLow() {
        return low;
    }

    public void setLow(Integer low) {
        this.low = low;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public Integer getHigh() {
        return high;
    }

    public void setHigh(Integer high) {
        this.high = high;
    }

    public static class CalorieRangeBuilder {
        private Integer low;
        private Integer mid;
        private Integer high;

        public CalorieRangeBuilder low(Integer low) {
            this.low = low;
            return this;
        }

        public CalorieRangeBuilder mid(Integer mid) {
            this.mid = mid;
            return this;
        }

        public CalorieRangeBuilder high(Integer high) {
            this.high = high;
            return this;
        }

        public CalorieRange build() {
            return new CalorieRange(low, mid, high);
        }
    }
}
