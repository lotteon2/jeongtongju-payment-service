package com.jeontongju.payment.exception;

public class RedisConnectionException extends RuntimeException{
    public RedisConnectionException() {
    }

    public RedisConnectionException(String message) {
        super(message);
    }

    public RedisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisConnectionException(Throwable cause) {
        super(cause);
    }

    public RedisConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
