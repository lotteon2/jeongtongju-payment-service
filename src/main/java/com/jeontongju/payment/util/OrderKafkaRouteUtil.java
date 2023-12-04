package com.jeontongju.payment.util;

import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.kafka.KafkaProcessor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaRouteUtil<T> extends KafkaProcessor<T> {
    private final String ORDER_TOPIC_NAME = "reduce-point";
    private final String COUPON_TOPIC_NAME = "use-coupon";
    private final String STOCK_TOPIC_NAME = "reduce-stock";

    public OrderKafkaRouteUtil(KafkaTemplate<String, T> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void send(T data){
        OrderInfoDto orderInfoDto = (OrderInfoDto) data;
        String topicName;

        if(orderInfoDto.getUserPointUpdateDto().getPoint() != null) { // 포인트 사용의 경우 포인트 서버로 보낸다
            topicName = ORDER_TOPIC_NAME;
        }else if(orderInfoDto.getUserCouponUpdateDto().getCouponCode() != null){ // 쿠폰 사용의 경우 쿠폰 서버로 보낸다(포인트 사용X)
            topicName = COUPON_TOPIC_NAME;
        }else{ // 그 외의 케이스
            topicName = STOCK_TOPIC_NAME;
        }

        super.send(topicName, data);
    }
}
