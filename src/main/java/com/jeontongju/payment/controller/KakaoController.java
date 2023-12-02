package com.jeontongju.payment.controller;

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
import com.jeontongju.payment.kafka.KafkaProcessor;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import com.jeontongju.payment.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final RedisUtil redisUtil;
    private final KafkaProcessor<OrderInfoDto> orderInfoDtoKafkaProcessor;
    private final PaymentService paymentService;

    private final String ORDER_TOPIC_NAME = "reduce-point";
    private final String COUPON_TOPIC_NAME = "use-coupon";
    private final String STOCK_TOPIC_NAME = "reduce-stock";

    @GetMapping("consumers/{consumerId}/credit-charge-history")
    public ResponseEntity<ResponseFormat<Page<CreditChargeHistoryDto>>> getConsumerCreditHistory(@PathVariable Long consumerId, Pageable pageable) {
        return ResponseEntity.ok().body(ResponseFormat.<Page<CreditChargeHistoryDto>>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.getReasonPhrase())
                .detail("크레딧 충전 내역 조회 성공")
                .data(paymentService.getConsumerCreditHistory(consumerId, pageable))
                .build());
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
        paymentService.createPayment(partnerOrderId, pgToken, redisUtil.commonApproveLogin(partnerOrderId, PaymentDto.class));
        return kakaoPayUtil.generatePageCloseCodeWithAlert(null);
    }

    @RequestMapping("/order-approve")
    public String kakaoOrderApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                                    @RequestParam("pg_token") String pgToken){
        OrderInfoDto orderInfoDto = redisUtil.commonApproveLogin(partnerOrderId, OrderInfoDto.class);
        orderInfoDto.getOrderCreationDto().setPgToken(pgToken);

        if(orderInfoDto.getUserPointUpdateDto().getPoint() != null) { // 포인트 사용의 경우 포인트 서버로 보낸다
            orderInfoDtoKafkaProcessor.send(ORDER_TOPIC_NAME, orderInfoDto);
        }else if(orderInfoDto.getUserCouponUpdateDto().getCouponCode() != null){ // 쿠폰 사용의 경우 쿠폰 서버로 보낸다(포인트 사용X)
            orderInfoDtoKafkaProcessor.send(COUPON_TOPIC_NAME, orderInfoDto);
        }else{ // 그 외의 케이스
            orderInfoDtoKafkaProcessor.send(STOCK_TOPIC_NAME, orderInfoDto);
        }

        return kakaoPayUtil.generatePageCloseCodeWithAlert(null);
    }

    @RequestMapping({"/fail", "/cancel"})
    public String kakaoFail(){
        return kakaoPayUtil.generateFailPage();
    }

    private void checkConsumerRole(MemberRoleEnum memberRole, String message) {
        if(memberRole != MemberRoleEnum.ROLE_USER){
            throw new InvalidPermissionException(message);
        }
    }
}
