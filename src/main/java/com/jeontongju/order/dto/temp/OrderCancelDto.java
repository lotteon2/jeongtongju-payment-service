package com.jeontongju.order.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelDto {
    private Long consumerId;
    private String ordersId;
    private String couponCode;
    private Long point;
    private Long cancelAmount;
}