package com.example.heatcalculate.controller;

import com.example.heatcalculate.exception.SessionExpiredException;
import com.example.heatcalculate.model.RefinedAnalysisResponse;
import com.example.heatcalculate.service.RefinedAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 精细分析控制器
 */
@RestController
@RequestMapping("/api/v1/calories/analyze/refined")
@Tag(name = "精细分析", description = "多轮追问精细热量分析接口")
public class RefinedAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(RefinedAnalysisController.class);

    private final RefinedAnalysisService refinedAnalysisService;

    public RefinedAnalysisController(RefinedAnalysisService refinedAnalysisService) {
        this.refinedAnalysisService = refinedAnalysisService;
    }

    /**
     * 开始精细分析
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "开始精细分析", description = "上传食物图片，开始多轮追问的精细热量分析")
    public ResponseEntity<RefinedAnalysisResponse> startRefinedAnalysis(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "note", required = false) String note) {

        log.info("收到精细分析请求，图片: {}, 备注: {}",
            image != null ? image.getOriginalFilename() : "null", note);

        RefinedAnalysisResponse response = refinedAnalysisService.startRefinedAnalysis(image, note);
        return ResponseEntity.ok(response);
    }

    /**
     * 继续精细分析（用户回答追问）
     */
    @PostMapping("/{sessionId}/continue")
    @Operation(summary = "继续精细分析", description = "提交用户回答，继续多轮追问")
    public ResponseEntity<RefinedAnalysisResponse> continueRefinedAnalysis(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {

        String answer = body.get("answer");
        if (answer == null || answer.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("继续精细分析，sessionId: {}", sessionId);

        RefinedAnalysisResponse response = refinedAnalysisService.continueRefinedAnalysis(sessionId, answer.trim());
        if (response == null) {
            throw new SessionExpiredException("会话不存在或已过期，请重新上传图片");
        }
        return ResponseEntity.ok(response);
    }
}
