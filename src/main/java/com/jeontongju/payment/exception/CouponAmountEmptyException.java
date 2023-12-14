package com.jeontongju.payment.exception;

public class CouponAmountEmptyException extends RuntimeException{
    public CouponAmountEmptyException() {
    }

    public CouponAmountEmptyException(String message) {
        super(message);
    }

    public CouponAmountEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CouponAmountEmptyException(Throwable cause) {
        super(cause);
    }

    public CouponAmountEmptyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
