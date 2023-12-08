package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentInfoDto {
    private Long minusPointAmount;
    private Long minusCouponAmount;
    private String couponCode;
}