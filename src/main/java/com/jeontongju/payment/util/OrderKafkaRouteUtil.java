package com.jeontongju.payment.util;

import com.jeontongju.payment.kafka.KafkaProcessor;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaRouteUtil<T> extends KafkaProcessor<T> {
    public OrderKafkaRouteUtil(KafkaTemplate<String, T> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void send(T data){
        OrderInfoDto orderInfoDto = (OrderInfoDto) data;
        String topicName;

        if(orderInfoDto.getUserPointUpdateDto().getPoint() > 0) { // 포인트 사용의 경우 포인트 서버로 보낸다
            topicName = KafkaTopicNameInfo.REDUCE_POINT;
        }else if(orderInfoDto.getUserCouponUpdateDto().getCouponCode() != null){ // 쿠폰 사용의 경우 쿠폰 서버로 보낸다(포인트 사용X)
            topicName = KafkaTopicNameInfo.USE_COUPON;
        }else{ // 그 외의 케이스
            topicName = KafkaTopicNameInfo.REDUCE_STOCK;
        }

        super.send(topicName, data);
    }
}
