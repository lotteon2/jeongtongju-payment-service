package com.jeontongju.payment.kafka;

import com.jeontongju.order.dto.temp.OrderCancelDto;
import com.jeontongju.payment.dto.temp.KakaoPayCancelDto;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaListenerProcessor {
    private final KakaoPayUtil kakaoPayUtil;
    private final PaymentService paymentService;
    private final String KAKAO_CANCEL_TOPIC = "cancel-kakaopay";
    private final String PAY_CANCEL_TOPIC = "cancel-order-payment";

    @KafkaListener(topics = KAKAO_CANCEL_TOPIC)
    public void cancelKakaoPay(KakaoPayCancelDto kakaoPayCancelDto) {
        kakaoPayUtil.callKakaoCancelApi(kakaoPayCancelDto);
    }

    @KafkaListener(topics = PAY_CANCEL_TOPIC)
    public void cancelPayment(OrderCancelDto orderCancelDto){
        paymentService.cancelPayment(orderCancelDto);
    }
}
