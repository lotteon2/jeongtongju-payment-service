package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 domain : coupon
 detail : 쿠폰 사용을 위한 DTO임 쿠폰 코드 확인 후 해당 쿠폰의 amount를 체크하고 쿠폰 사용을 한다
 method : kafka
 comment :
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponUpdateDto {
    private Long consumerId;
    private String couponCode;
    private Long couponAmount;
}