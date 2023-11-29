package com.jeontongju.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.MemberCreditChargeDto;
import com.jeontongju.payment.dto.PaymentCreationDto;
import com.jeontongju.payment.dto.PaymentDto;
import com.jeontongju.payment.dto.response.CreditChargeHistoryDto;
import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.dto.temp.ResponseFormat;
import com.jeontongju.payment.enums.temp.MemberRoleEnum;
import com.jeontongju.payment.exception.CouponAmountEmptyException;
import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Slf4j
public class KakaoController {
    private final KakaoPayUtil kakaoPayUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, OrderInfoDto> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final String ORDER_TOPIC_NAME = "reduce-point";

    @GetMapping("consumers/{consumerId}/credit-charge-history")
    public ResponseFormat<Page<CreditChargeHistoryDto>> getConsumerCreditHistory(@PathVariable Long consumerId, Pageable pageable) {
        return ResponseFormat.<Page<CreditChargeHistoryDto>>builder()
                .code(HttpStatus.OK)
                .detail(HttpStatus.OK.name())
                .message("크레딧 충전 내역 조회 성공")
                .data(paymentService.getConsumerCreditHistory(consumerId, pageable))
                .build();
    }

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestHeader Long memberId,
                                              @RequestHeader MemberRoleEnum memberRole,
                                              @RequestBody @Valid PaymentCreationDto paymentCreationDto) {
        checkConsumerRole(memberRole, "주문은 소비자만 할 수 있습니다.");
        if(paymentCreationDto.getCouponCode()!=null && paymentCreationDto.getCouponAmount()==null
        || paymentCreationDto.getCouponCode()==null && paymentCreationDto.getCouponAmount()!=null){
            throw new CouponAmountEmptyException("쿠폰 관련 정보가 이상합니다.");
        }

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

    @RequestMapping("/credit-approve")
    public String kakaoCreditApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                               @RequestParam("pg_token") String pgToken){
        paymentService.createPayment(partnerOrderId, pgToken, commonApproveLogin(partnerOrderId, PaymentDto.class));
        return kakaoPayUtil.generatePageCloseCodeWithAlert(null);
    }

    @RequestMapping("/order-approve")
    public String kakaoOrderApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                               @RequestParam("pg_token") String pgToken){
        OrderInfoDto orderInfoDto = commonApproveLogin(partnerOrderId, OrderInfoDto.class);
        orderInfoDto.getOrderCreationDto().setPgToken(pgToken);

        kafkaTemplate.send(ORDER_TOPIC_NAME, orderInfoDto);
        return kakaoPayUtil.generatePageCloseCodeWithAlert(null);
    }

    @RequestMapping({"/fail", "/cancel"})
    public String kakaoFail(){
        return kakaoPayUtil.generateFailPage();
    }

    public <T> T commonApproveLogin(String partnerOrderId, Class<T> valueType) {
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        try {
            String jsonValue = vop.get(partnerOrderId);
            return objectMapper.readValue(jsonValue, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("파싱 에러", e);
        }
    }

    private void checkConsumerRole(MemberRoleEnum memberRole, String message) {
        if(memberRole != MemberRoleEnum.consumer){
            throw new InvalidPermissionException(message);
        }
    }
}
