package com.jeontongju.payment.service;

import com.jeontongju.payment.kafka.KafkaListenerProcessor;
import com.jeontongju.payment.util.KakaoPayUtil;
import com.jeontongju.payment.util.RedisUtil;
import io.github.bitbox.bitbox.dto.KakaoPayMethod;
import io.github.bitbox.bitbox.dto.OrderCreationDto;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.dto.UserCouponUpdateDto;
import io.github.bitbox.bitbox.dto.UserPointUpdateDto;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class PaymentServiceTest {
    @MockBean
    private KakaoPayUtil kakaoPayUtil;
    @MockBean
    private RedisUtil redisUtil;
    @MockBean
    private KafkaListenerProcessor kafkaListenerProcessor;
    @Autowired
    private PaymentService paymentService;

    @Test
    public void payment테이블이_정상적으로_생성된다(){
        UserPointUpdateDto userPointUpdateDto = UserPointUpdateDto.builder().point(100L).consumerId(1L).build();
        UserCouponUpdateDto userCouponUpdateDto = UserCouponUpdateDto.builder().consumerId(1L).couponCode("test").couponAmount(100L).totalAmount(10L).build();
        OrderCreationDto orderCreationDto = OrderCreationDto.builder()
                .totalPrice(10L).consumerId(1L).paymentType(PaymentTypeEnum.ORDER).paymentMethod(PaymentMethodEnum.KAKAO)
                .paymentInfo(KakaoPayMethod.builder().partnerOrderId("test").tid("123").partnerUserId("test").pgToken("test").build()).orderId("test").build();
        OrderInfoDto orderInfoDto = OrderInfoDto.builder().userPointUpdateDto(userPointUpdateDto)
                .userCouponUpdateDto(userCouponUpdateDto).orderCreationDto(orderCreationDto).build();

        paymentService.createOrderPaymentInfo(orderInfoDto);
    }
}