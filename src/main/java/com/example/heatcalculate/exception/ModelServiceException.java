package com.example.heatcalculate.exception;

/**
 * 模型服务异常
 */
public class ModelServiceException extends RuntimeException {

    public ModelServiceException(String message) {
        super(message);
    }

    public ModelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
