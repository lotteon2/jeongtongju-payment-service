package com.jeontongju.payment.feign;

import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.UserCouponUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="coupon-service")
public interface CouponFeignServiceClient {
    // 해당 Feign에서는 쿠폰이 유효한지 또한 해당쿠폰을 컨슈머가 가지고있는게 맞는지 또한 쿠폰액이 전체금액의 10%를 초과하지 않았는지 체크
    @GetMapping("/coupons")
    FeignFormat<Void> checkCouponInfo(@RequestBody UserCouponUpdateDto userCouponUpdateDto);
}