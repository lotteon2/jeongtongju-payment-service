package com.jeontongju.payment.exception;

public class KakaoPayException extends RuntimeException{
    public KakaoPayException() {
    }

    public KakaoPayException(String message) {
        super(message);
    }

    public KakaoPayException(String message, Throwable cause) {
        super(message, cause);
    }

    public KakaoPayException(Throwable cause) {
        super(cause);
    }

    public KakaoPayException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
