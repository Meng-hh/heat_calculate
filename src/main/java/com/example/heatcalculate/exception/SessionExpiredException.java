package com.example.heatcalculate.exception;

/**
 * 会话过期或不存在异常
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
