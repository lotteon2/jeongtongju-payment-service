package com.jeontongju.payment.controller;

import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.KakaoPayApproveDto;
import com.jeontongju.payment.dto.temp.KakaoPayMethod;
import com.jeontongju.payment.dto.temp.PaymentMethod;
import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/")
@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentFeignController {
    private final KakaoPayUtil kakaoPayUtil;

    @PostMapping("pay-approve")
    public FeignFormat<Void> approveKakaopay(@RequestParam("paymentMethodEnum") PaymentMethodEnum paymentMethodEnum, @RequestBody PaymentMethod paymentMethod) {
        if(paymentMethodEnum == PaymentMethodEnum.KAKAO){
            KakaoPayMethod kakaoPayMethod = (KakaoPayMethod) paymentMethod;
            kakaoPayUtil.callKakaoApproveApi(KakaoPayApproveDto.builder()
                            .tid(kakaoPayMethod.getTid())
                            .pgToken(kakaoPayMethod.getPgToken())
                            .partnerOrderId(kakaoPayMethod.getPartnerOrderId())
                            .partnerUserId(kakaoPayMethod.getPartnerUserId())
                    .build());
        }
        return FeignFormat.<Void>builder()
                .code(HttpStatus.SC_OK)
                .build();
    }
}