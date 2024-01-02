package com.jeontongju.payment.exception;

import io.github.bitbox.bitbox.enums.FailureTypeEnum;
import lombok.Getter;

@Getter
public class FeignClientResponseException extends RuntimeException{
    public FailureTypeEnum failureType;

    public FeignClientResponseException(FailureTypeEnum failureType) {
        this.failureType=failureType;
    }
}
