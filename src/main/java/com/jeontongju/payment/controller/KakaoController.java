package com.jeontongju.payment.controller;

import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.MemberCreditChargeDto;
import com.jeontongju.payment.dto.PaymentCreationDto;
import com.jeontongju.payment.dto.CreditPaymentDto;
import com.jeontongju.payment.dto.SubscriptionPaymentDto;
import com.jeontongju.payment.dto.response.CreditChargeHistoryDto;
import com.jeontongju.payment.dto.SubscriptionRequestDto;
import com.jeontongju.payment.exception.CouponAmountEmptyException;
import com.jeontongju.payment.exception.FeignClientResponseException;
import com.jeontongju.payment.exception.InvalidPermissionException;
import com.jeontongju.payment.feign.PointFeignServiceClient;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import com.jeontongju.payment.util.OrderKafkaRouteUtil;
import com.jeontongju.payment.util.RedisUtil;
import io.github.bitbox.bitbox.dto.FeignFormat;
import io.github.bitbox.bitbox.dto.KakaoPayMethod;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.dto.ResponseFormat;
import io.github.bitbox.bitbox.enums.FailureTypeEnum;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class KakaoController {
    private final KakaoPayUtil kakaoPayUtil;
    private final RedisUtil redisUtil;
    private final OrderKafkaRouteUtil<OrderInfoDto> orderInfoDtoKafkaRouteUtil;
    private final PaymentService paymentService;
    private final PointFeignServiceClient pointFeignServiceClient;
    @Value("${subscriptionFee}")
    private Long subscriptionFee;

    @GetMapping("consumers/{consumerId}/credit-charge-history")
    public ResponseEntity<ResponseFormat<Page<CreditChargeHistoryDto>>> getConsumerCreditHistory(@PathVariable Long consumerId, Pageable pageable) {
        return ResponseEntity.ok().body(ResponseFormat.<Page<CreditChargeHistoryDto>>builder().code(HttpStatus.OK.value()).message(HttpStatus.OK.getReasonPhrase())
                .detail("크레딧 충전 내역 조회 성공").data(paymentService.getConsumerCreditHistory(consumerId, pageable)).build());
    }

    @PostMapping("/subscription")
    public ResponseEntity<String> createSubscription(@RequestHeader Long memberId, @RequestBody @Valid SubscriptionRequestDto subscriptionRequestDto){
        // 이미 구독권이 존재하는 경우
        if(pointFeignServiceClient.getConsumerSubscription(memberId).getData()){
            throw new FeignClientResponseException(FailureTypeEnum.EXISTING_SUBSCRIPTION_PAYMENT);
        }

        ResponseEntity<String> result = null;
        if(subscriptionRequestDto.getPaymentMethod() == PaymentMethodEnum.KAKAO) {
            result = kakaoPayUtil.createSubscription(KakaoPaymentDto.convertPaymentDto(String.valueOf(memberId), subscriptionRequestDto.getItemName(), subscriptionFee));
        }

        return result;
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

    @RequestMapping("/subscription-approve")
    public String kakaoSubscriptionApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                                     @RequestParam("pg_token") String pgToken){
        paymentService.createSubscription(partnerOrderId, pgToken, redisUtil.commonApproveLogin(partnerOrderId, SubscriptionPaymentDto.class));
        return kakaoPayUtil.generatePageCloseCodeWithAlert("subscribe");
    }

    @RequestMapping("/credit-approve")
    public String kakaoCreditApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                               @RequestParam("pg_token") String pgToken){
        paymentService.createPayment(partnerOrderId, pgToken, redisUtil.commonApproveLogin(partnerOrderId, CreditPaymentDto.class));
        return kakaoPayUtil.generatePageCloseCodeWithAlert("credit");
    }

    @RequestMapping("/order-approve")
    public String kakaoOrderApprove(@RequestParam("partnerOrderId") String partnerOrderId,
                                    @RequestParam("pg_token") String pgToken){
        OrderInfoDto orderInfoDto = redisUtil.commonApproveLogin(partnerOrderId, OrderInfoDto.class);
        KakaoPayMethod kakaoPayMethod = (KakaoPayMethod) orderInfoDto.getOrderCreationDto().getPaymentInfo();
        kakaoPayMethod.setPgToken(pgToken);

        orderInfoDtoKafkaRouteUtil.send(orderInfoDto);
        return kakaoPayUtil.generatePageCloseCodeWithAlert("pay");
    }

    @RequestMapping({"/fail", "/cancel"})
    public String kakaoFail(){
        return kakaoPayUtil.generateFailPage();
    }

    private void checkConsumerRole(MemberRoleEnum memberRole, String message) {
        if(memberRole != MemberRoleEnum.ROLE_CONSUMER){
            throw new InvalidPermissionException(message);
        }
    }
}
