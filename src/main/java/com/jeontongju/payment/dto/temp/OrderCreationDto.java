package com.jeontongju.payment.dto.temp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
     domain : payment, order
     detail : 주문정보를 담고 있는 DTO
     method :
     comment : 결제 QR을 만들기 위해서 payment가 먼저 요청을 받고 redis에 받은 정보들을 저장해둔다. 그리고 approve url을 order-service로
               보내고 해당 order-service에서 redis에 저장한 값을 꺼내고 order table을 insert한다.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderCreationDto {
    private PaymentCreationDto paymentCreationDto;
    private String tid;
    private String consumerId;
}
