package com.example.heatcalculate.controller;

import com.example.heatcalculate.model.CalorieResult;
import com.example.heatcalculate.model.CorrectionRequest;
import com.example.heatcalculate.service.CorrectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 结果纠正控制器
 */
@RestController
@RequestMapping("/api/v1/calories/analyze/sessions")
@Tag(name = "结果纠正", description = "纠正食物识别结果并重新计算热量")
public class CorrectionController {

    private static final Logger log = LoggerFactory.getLogger(CorrectionController.class);

    private final CorrectionService correctionService;

    public CorrectionController(CorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    /**
     * 纠正分析结果
     */
    @PostMapping("/{sessionId}/correct")
    @Operation(summary = "纠正分析结果", description = "修改食物名称/重量或补充备注，触发模型重新计算热量")
    public ResponseEntity<CalorieResult> correctResult(
            @PathVariable String sessionId,
            @RequestBody CorrectionRequest request) {

        log.info("收到纠正请求，sessionId: {}, corrections: {}, note: {}",
            sessionId,
            request.getCorrections() != null ? request.getCorrections().size() : 0,
            request.getAdditionalNote());

        CalorieResult result = correctionService.correct(
            sessionId,
            request.getCorrections(),
            request.getAdditionalNote()
        );

        return ResponseEntity.ok(result);
    }
}
