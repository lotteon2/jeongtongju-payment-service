package com.jeontongju.payment.exception;

public class InvalidPermissionException extends RuntimeException{
    public InvalidPermissionException() {
        super();
    }

    public InvalidPermissionException(String message) {
        super(message);
    }

    public InvalidPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPermissionException(Throwable cause) {
        super(cause);
    }

    protected InvalidPermissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
