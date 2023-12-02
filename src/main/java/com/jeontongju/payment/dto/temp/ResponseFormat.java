package com.jeontongju.payment.dto.temp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 domain : all
 detail : Rest 통신시 사용되는 Success Format Dto
 method :
 comment :
 */

@Getter
@Builder
@AllArgsConstructor
public class ResponseFormat<T> {
    private int code;
    private String message;
    private String detail;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String failure;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;
}