package com.example.heatcalculate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分析请求
 */
@Schema(description = "食物图片分析请求")
public class AnalysisRequest {

    @Schema(description = "食物图片（JPG/PNG/WEBP，最大10MB）", required = true)
    private MultipartFile image;

    @Schema(description = "可选备注信息", example = "这是一份午餐")
    private String note;

    public AnalysisRequest() {
    }

    public AnalysisRequest(MultipartFile image, String note) {
        this.image = image;
        this.note = note;
    }

    public static AnalysisRequestBuilder builder() {
        return new AnalysisRequestBuilder();
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public static class AnalysisRequestBuilder {
        private MultipartFile image;
        private String note;

        public AnalysisRequestBuilder image(MultipartFile image) {
            this.image = image;
            return this;
        }

        public AnalysisRequestBuilder note(String note) {
            this.note = note;
            return this;
        }

        public AnalysisRequest build() {
            return new AnalysisRequest(image, note);
        }
    }
}
