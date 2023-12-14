package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
     domain : payment, order, consumer, coupon, product
     detail : 주문정보를 담고 있는 DTO
     method :
     comment : 결제 QR을 만들기 위해서 payment가 먼저 요청을 받고 redis에 받은 정보들을 저장해둔다. 그리고 approve시점에 해당 Dto를 카프카로 전송한다
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoDto {
    private UserPointUpdateDto userPointUpdateDto; // 포인트
    private UserCouponUpdateDto userCouponUpdateDto; // 쿠폰
    private List<ProductUpdateDto> productUpdateDto; // 재고
    private OrderCreationDto orderCreationDto; // 주문정보(결제 포함)
}
