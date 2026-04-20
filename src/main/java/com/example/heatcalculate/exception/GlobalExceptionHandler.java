package com.example.heatcalculate.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<ErrorResponse> handleImageValidationException(ImageValidationException e) {
        log.warn("图片校验失败: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(e.getMessage())
                .build());
    }

    @ExceptionHandler(ModelServiceException.class)
    public ResponseEntity<ErrorResponse> handleModelServiceException(ModelServiceException e) {
        log.error("模型服务异常: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.builder()
                .code(HttpStatus.BAD_GATEWAY.value())
                .message("模型服务暂时不可用，请稍后重试")
                .build());
    }

    @ExceptionHandler(ModelParseException.class)
    public ResponseEntity<ErrorResponse> handleModelParseException(ModelParseException e) {
        log.error("模型输出解析失败: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("模型输出解析失败")
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("系统内部错误")
                .build());
    }

    /**
     * 错误响应
     */
    @Schema(description = "错误响应")
    public static class ErrorResponse {
        @Schema(description = "错误码", example = "400")
        private Integer code;

        @Schema(description = "错误信息", example = "图片大小不能超过10MB")
        private String message;

        public ErrorResponse() {
        }

        public ErrorResponse(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static class ErrorResponseBuilder {
            private Integer code;
            private String message;

            public ErrorResponseBuilder code(Integer code) {
                this.code = code;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(code, message);
            }
        }
    }
}
