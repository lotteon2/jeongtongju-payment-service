package com.jeontongju.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.PaymentDto;
import com.jeontongju.payment.dto.controller.MemberCreditChargeDto;
import com.jeontongju.payment.dto.temp.PaymentCreationDto;
import com.jeontongju.payment.enums.temp.MemberRoleEnum;
import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final KakaoPayUtil kakaoPayUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestHeader Long memberId,
                                              @RequestHeader MemberRoleEnum memberRole,
                                              @RequestBody @Valid PaymentCreationDto paymentCreationDto) {
        checkConsumerRole(memberRole, "주문은 소비자만 할 수 있습니다.");

        return kakaoPayUtil.createOrderInfoWithKakao(paymentCreationDto, KakaoPaymentDto.convertPaymentDto(String.valueOf(memberId),
                paymentCreationDto.getTitleName(), paymentCreationDto.getTotalAmount()));
    }

    @PostMapping("/credit")
    public ResponseEntity<String> chargeCredit(@RequestHeader Long memberId,
                                               @RequestHeader MemberRoleEnum memberRole,
                                               @RequestBody @Valid MemberCreditChargeDto memberCreditChargeDto) {
        checkConsumerRole(memberRole, "크레딧 충전은 소비자만 할 수 있습니다.");

        return kakaoPayUtil.createCreditInfoWithKakao(memberCreditChargeDto, KakaoPaymentDto.convertPaymentDto(String.valueOf(memberId),
                memberCreditChargeDto.getItemName(), memberCreditChargeDto.getChargeCredit()));
    }

    @RequestMapping("/approve")
    public String kakaoApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                               @RequestParam("pg_token") String pgToken){
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        PaymentDto paymentDto;
        try {
            paymentDto = objectMapper.readValue(vop.get(partnerOrderId), PaymentDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("파싱 에러");
        }

        paymentService.createPayment(partnerOrderId, pgToken, paymentDto);
        return null;
    }

    private void checkConsumerRole(MemberRoleEnum memberRole, String message) {
        if(memberRole != MemberRoleEnum.consumer){
            throw new InvalidPermissionException(message);
        }
    }
}
