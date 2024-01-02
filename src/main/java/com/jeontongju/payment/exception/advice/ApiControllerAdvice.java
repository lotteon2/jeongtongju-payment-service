package com.jeontongju.payment.exception.advice;

import com.jeontongju.payment.exception.CouponAmountEmptyException;
import com.jeontongju.payment.exception.FeignClientResponseException;
import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.exception.KakaoPayApproveException;
import com.jeontongju.payment.exception.KakaoPayException;
import com.jeontongju.payment.exception.RedisConnectionException;
import com.jeontongju.payment.exception.response.ErrorResponse;
import com.jeontongju.payment.exception.response.KakaoErrorResponse;
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

    @ExceptionHandler(CouponAmountEmptyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCouponAmountEmptyException(CouponAmountEmptyException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ErrorResponse.builder()
                .message(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .build();
    }

    @ExceptionHandler(KakaoPayException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleKakaoPayException(KakaoPayException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(RedisConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleMethodRedirectException(RedisConnectionException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(KafkaException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleMethodRedirectException(KafkaException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(KakaoPayApproveException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleMethodRedirectException(KakaoPayApproveException e) {
        return ErrorResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(FeignClientResponseException.class)
    @ResponseStatus(HttpStatus.OK)
    public KakaoErrorResponse handleFeignClientResponseException(FeignClientResponseException e){
        return KakaoErrorResponse.builder()
                .message(e.getFailureType())
        .build();
    }
}