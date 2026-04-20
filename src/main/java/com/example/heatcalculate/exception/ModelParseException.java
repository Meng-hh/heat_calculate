package com.example.heatcalculate.exception;

/**
 * 模型输出解析异常
 */
public class ModelParseException extends RuntimeException {

    public ModelParseException(String message) {
        super(message);
    }

    public ModelParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
