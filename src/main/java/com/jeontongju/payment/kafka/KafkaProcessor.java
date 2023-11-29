package com.jeontongju.payment.kafka;

import com.jeontongju.payment.dto.temp.KakaoPayCancelDto;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProcessor {
    private final KakaoPayUtil kakaoPayUtil;
    private final String KAKAO_CANCEL_TOPIC = "cancel-kakaopay";

    @KafkaListener(topics = KAKAO_CANCEL_TOPIC)
    public void cancelKakaoPay(KakaoPayCancelDto kakaoPayCancelDto) {
        kakaoPayUtil.callKakaoCancelApi(kakaoPayCancelDto);
    }
}
