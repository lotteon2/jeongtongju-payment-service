package com.jeontongju.payment.exception.advice;

import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.exception.KakaoPayApproveException;
import com.jeontongju.payment.exception.RedisConnectionException;
import com.jeontongju.payment.exception.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.KafkaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiControllerAdvice {
    @ExceptionHandler(InvalidPermissionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(InvalidPermissionException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ErrorResponse.builder()
                .message(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .build();
    }

    @ExceptionHandler(RedisConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    protected ErrorResponse handleMethodRedirectException(RedisConnectionException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(KafkaException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    protected ErrorResponse handleMethodRedirectException(KafkaException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(KakaoPayApproveException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    protected ErrorResponse handleMethodRedirectException(KakaoPayApproveException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }
}