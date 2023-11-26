package com.jeontongju.payment.controller;

import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.temp.PaymentCreationDto;
import com.jeontongju.payment.enums.temp.MemberRoleEnum;
import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final KakaoPayUtil kakaoPayUtil;
    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestHeader Long memberId,
                                              @RequestHeader MemberRoleEnum memberRole,
                                              @RequestBody @Valid PaymentCreationDto paymentCreationDto) {
        if(memberRole != MemberRoleEnum.consumer){
            throw new InvalidPermissionException("주문은 소비자만 할 수 있습니다.");
        }

        return kakaoPayUtil.callKakaoReadyApi(paymentCreationDto, KakaoPaymentDto.convertOrderDtoToPaymentDto(String.valueOf(memberId),
                paymentCreationDto.getTitleName(), paymentCreationDto.getTotalAmount()));
    }
}
