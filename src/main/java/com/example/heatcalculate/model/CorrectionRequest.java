package com.example.heatcalculate.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 纠正请求
 */
@Schema(description = "结果纠正请求")
public class CorrectionRequest {

    @Schema(description = "纠正项列表")
    private List<CorrectionItem> corrections;

    @Schema(description = "补充说明")
    private String additionalNote;

    public CorrectionRequest() {
    }

    public List<CorrectionItem> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<CorrectionItem> corrections) {
        this.corrections = corrections;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }

    public void setAdditionalNote(String additionalNote) {
        this.additionalNote = additionalNote;
    }

    /**
     * 单项纠正
     */
    @Schema(description = "单项食物纠正")
    public static class CorrectionItem {

        @Schema(description = "食物索引（从0开始）")
        private int index;

        @Schema(description = "纠正后的食物名称")
        private String name;

        @Schema(description = "纠正后的重量")
        private String weight;

        public CorrectionItem() {
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }
    }
}
