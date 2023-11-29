package com.jeontongju.payment.exception;

public class KakaoPayApproveException extends RuntimeException{
    public KakaoPayApproveException() {
    }

    public KakaoPayApproveException(String message) {
        super(message);
    }

    public KakaoPayApproveException(String message, Throwable cause) {
        super(message, cause);
    }

    public KakaoPayApproveException(Throwable cause) {
        super(cause);
    }

    public KakaoPayApproveException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
