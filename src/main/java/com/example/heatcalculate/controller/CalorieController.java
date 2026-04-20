package com.example.heatcalculate.controller;

import com.example.heatcalculate.model.CalorieResult;
import com.example.heatcalculate.service.CalorieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 热量分析控制器
 */
@RestController
@RequestMapping("/api/v1/calories")
@Tag(name = "热量分析", description = "食物图片热量识别接口")
public class CalorieController {

    private static final Logger log = LoggerFactory.getLogger(CalorieController.class);

    private final CalorieService calorieService;

    public CalorieController(CalorieService calorieService) {
        this.calorieService = calorieService;
    }

    /**
     * 分析食物图片并返回热量估算
     *
     * @param image 食物图片
     * @param note  可选备注
     * @return 热量识别结果
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "分析食物图片热量",
        description = "上传食物图片，系统通过AI识别食物种类并估算热量区间"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "识别成功",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CalorieResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误（图片格式不支持或大小超过10MB）",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = com.example.heatcalculate.exception.GlobalExceptionHandler.ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "502",
            description = "模型服务暂时不可用",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = com.example.heatcalculate.exception.GlobalExceptionHandler.ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "系统内部错误",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = com.example.heatcalculate.exception.GlobalExceptionHandler.ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<CalorieResult> analyzeFood(
        @Parameter(description = "食物图片（JPG/PNG/WEBP，最大10MB）", required = true)
        @RequestParam("image") MultipartFile image,
        
        @Parameter(description = "可选备注信息")
        @RequestParam(value = "note", required = false) String note) {
        
        log.info("收到热量分析请求，图片: {}, 备注: {}", 
            image != null ? image.getOriginalFilename() : "null", 
            note);
        
        CalorieResult result = calorieService.analyzeFood(image, note);
        return ResponseEntity.ok(result);
    }
}
