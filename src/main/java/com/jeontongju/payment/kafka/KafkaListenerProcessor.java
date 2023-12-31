package com.jeontongju.payment.kafka;

import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import io.github.bitbox.bitbox.dto.KakaoPayCancelDto;
import io.github.bitbox.bitbox.dto.OrderCancelDto;
import io.github.bitbox.bitbox.dto.SubscriptionBatchDto;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaListenerProcessor {
    private final KakaoPayUtil kakaoPayUtil;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, OrderCancelDto> orderCancelDtoKafkaTemplate;

    @KafkaListener(topics = KafkaTopicNameInfo.CANCEL_KAKAOPAY)
    public void cancelKakaoPay(KakaoPayCancelDto kakaoPayCancelDto) {
        kakaoPayUtil.callKakaoCancelApi(kakaoPayCancelDto);
    }

    @KafkaListener(topics = KafkaTopicNameInfo.CANCEL_ORDER_PAYMENT)
    public void cancelPayment(OrderCancelDto orderCancelDto){
        try {
            paymentService.cancelPayment(orderCancelDto);
        }catch(Exception e){
            orderCancelDtoKafkaTemplate.send(KafkaTopicNameInfo.RECOVER_CANCEL_ORDER_STOCK, orderCancelDto);
        }
    }

    @KafkaListener(topics = KafkaTopicNameInfo.PAYMENT_SUBSCRIPTION)
    public void renewSubscription(SubscriptionBatchDto subscriptionBatchDto){
        kakaoPayUtil.renewSubscription(subscriptionBatchDto.getSubscriptionBatchInterface());
    }
}
