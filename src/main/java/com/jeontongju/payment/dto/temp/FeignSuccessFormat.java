package com.jeontongju.payment.dto.temp;

import lombok.Builder;
import lombok.Data;

@Data
public class FeignSuccessFormat {
    private final Long code;
    private final Object message;

    @Builder
    public FeignSuccessFormat(Long code, Object message) {
        this.code=code;
        this.message = message;
    }
}
