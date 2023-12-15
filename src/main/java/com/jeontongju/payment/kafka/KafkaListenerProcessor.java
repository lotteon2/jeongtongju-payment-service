package com.jeontongju.payment.kafka;

import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import io.github.bitbox.bitbox.dto.KakaoPayCancelDto;
import io.github.bitbox.bitbox.dto.OrderCancelDto;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaListenerProcessor {
    private final KakaoPayUtil kakaoPayUtil;
    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaTopicNameInfo.CANCEL_KAKAOPAY)
    public void cancelKakaoPay(KakaoPayCancelDto kakaoPayCancelDto) {
        kakaoPayUtil.callKakaoCancelApi(kakaoPayCancelDto);
    }

    @KafkaListener(topics = KafkaTopicNameInfo.CANCEL_ORDER_PAYMENT)
    public void cancelPayment(OrderCancelDto orderCancelDto){
        paymentService.cancelPayment(orderCancelDto);
    }
}
