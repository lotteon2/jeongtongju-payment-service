package com.jeontongju.payment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.MemberCreditChargeDto;
import com.jeontongju.payment.dto.PaymentCreationDto;
import com.jeontongju.payment.dto.CreditPaymentDto;
import com.jeontongju.payment.dto.SubscriptionPaymentDto;
import com.jeontongju.payment.exception.FeignClientResponseException;
import com.jeontongju.payment.feign.CouponFeignServiceClient;
import com.jeontongju.payment.feign.PointFeignServiceClient;
import com.jeontongju.payment.feign.ProductFeignServiceClient;
import io.github.bitbox.bitbox.dto.FeignFormat;
import io.github.bitbox.bitbox.dto.KakaoPayApproveDto;
import io.github.bitbox.bitbox.dto.KakaoPayCancelDto;
import io.github.bitbox.bitbox.dto.KakaoPayMethod;
import io.github.bitbox.bitbox.dto.OrderCreationDto;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.dto.ProductInfoDto;
import io.github.bitbox.bitbox.dto.ProductSearchDto;
import io.github.bitbox.bitbox.dto.ProductUpdateDto;
import io.github.bitbox.bitbox.dto.UserCouponUpdateDto;
import io.github.bitbox.bitbox.dto.UserPointUpdateDto;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoPayUtil {
    @Value("${cid}")
    private String cid;

    @Value("${subscriptionCid}")
    private String subscriptionCid;

    @Value("${orderApprovalUrl}")
    private String orderApprovalUrl;

    @Value("${orderCancelUrl}")
    private String orderCancelUrl;

    @Value("${orderFailUrl}")
    private String orderFailUrl;

    @Value("${creditApprovalUrl}")
    private String creditApprovalUrl;

    @Value("${creditCancelUrl}")
    private String creditCancelUrl;

    @Value("${creditFailUrl}")
    private String creditFailUrl;

    @Value("${subscriptionApprovalUrl}")
    private String subscriptionApprovalUrl;

    @Value("${subscriptionCancelUrl}")
    private String subscriptionCancelUrl;

    @Value("${subscriptionFailUrl}")
    private String subscriptionFailUrl;

    @Value("${kakaoPayKey}")
    private String kakaoPayKey;

    private final String KAKAO_READY_URL = "https://kapi.kakao.com/v1/payment/ready";

    private final String KAKAO_APPROVE_URL = "https://kapi.kakao.com/v1/payment/approve";

    private final String KAKAO_CANCEL_URL = "https://kapi.kakao.com/v1/payment/cancel";

    private final ProductFeignServiceClient productFeignServiceClient;
    private final PointFeignServiceClient pointFeignServiceClient;
    private final CouponFeignServiceClient couponFeignServiceClient;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public ResponseEntity<String> createSubscription(KakaoPaymentDto kakaoPaymentDto){
        ResponseEntity<String> exchange = callKakaoApi( KAKAO_READY_URL, kakaoPaymentDto.generateKakaoPayApprovePayReady(subscriptionCid,kakaoPaymentDto.getTotalAmount(),subscriptionApprovalUrl, subscriptionCancelUrl, subscriptionFailUrl ));
        redisUtil.saveRedis(kakaoPaymentDto.getPartnerOrderId(), SubscriptionPaymentDto.builder()
                .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                .paymentType(PaymentTypeEnum.SUBSCRIPTION)
                .paymentAmount(kakaoPaymentDto.getTotalAmount())
                .paymentMethod(PaymentMethodEnum.KAKAO)
                .paymentTaxFreeAmount(0L)
                .tid(getTargetToken(exchange,"tid"))
                .build());
        return exchange;
    }

    public ResponseEntity<String> createOrderInfoWithKakao(PaymentCreationDto paymentCreationDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, kakaoPaymentDto.generateKakaoPayApprovePayReady(cid,paymentCreationDto.getRealAmount(),orderApprovalUrl, orderCancelUrl, orderFailUrl ));

        // ProductUpdateDto 만드는 표현식(Feign 및 재고차감 하라는 요청을 보낼때 사용)
        List<ProductUpdateDto> productSearchDtoList = paymentCreationDto.getProducts().stream()
                .map(productDto -> ProductUpdateDto.builder()
                        .productId(productDto.getProductId())
                        .productCount(productDto.getProductCount())
                        .build())
                .collect(Collectors.toList());

        // Product에 Feign을 요청한다. (재고가 없거나 프론트에서 넘겨준 상품의 총가격과 상품 가격이 다르면 약속된 비정상 포맷을 반환한다)
        FeignFormat<List<ProductInfoDto>> productInfo = productFeignServiceClient.getProductInfo(ProductSearchDto.builder()
                .productUpdateDtoList(productSearchDtoList).totalPrice(paymentCreationDto.getTotalAmount()).build());
        checkValidationCondition(productInfo);

        if(paymentCreationDto.getPointUsageAmount()!=null) { // optional
            // Consumer에 Feign을 요청한다. (포인트가 부족한 경우는 예외 발생한다)
            checkValidationCondition(pointFeignServiceClient.checkConsumerPoint(UserPointUpdateDto.builder()
                    .point(paymentCreationDto.getPointUsageAmount())
                    .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                    .totalAmount(paymentCreationDto.getTotalAmount())
                    .build()));
        }

        if(paymentCreationDto.getCouponCode()!=null) { // optional
            // coupon에 Feign을 요청한다. (쿠폰을 사용할 수 없는 경우는 예외 발생한다)
            checkValidationCondition(couponFeignServiceClient.checkCouponInfo(UserCouponUpdateDto.builder()
                    .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                    .couponCode(paymentCreationDto.getCouponCode())
                    .couponAmount(paymentCreationDto.getCouponAmount())
                    .totalAmount(paymentCreationDto.getTotalAmount())
                    .build()));
        }

        Long consumerId = Long.valueOf(kakaoPaymentDto.getPartnerUserId());
        redisUtil.saveRedis(kakaoPaymentDto.getPartnerOrderId(), OrderInfoDto.builder()
                .userPointUpdateDto(UserPointUpdateDto.builder().consumerId(consumerId).point(paymentCreationDto.getPointUsageAmount()).totalAmount(paymentCreationDto.getTotalAmount()).build())
                .userCouponUpdateDto(UserCouponUpdateDto.builder().consumerId(consumerId).couponCode(paymentCreationDto.getCouponCode())
                        .couponAmount(paymentCreationDto.getCouponAmount()).totalAmount(paymentCreationDto.getTotalAmount()).build())
                .productUpdateDto(productSearchDtoList)
                .orderCreationDto(OrderCreationDto.builder()
                        .totalPrice(paymentCreationDto.getTotalAmount())
                        .consumerId(consumerId)
                        .orderDate(LocalDateTime.now())
                        .orderId(kakaoPaymentDto.getPartnerOrderId())
                        .paymentType(PaymentTypeEnum.ORDER)
                        .paymentMethod(PaymentMethodEnum.KAKAO)
                        .paymentInfo(KakaoPayMethod.builder().tid(getTargetToken(exchange,"tid")).partnerUserId(String.valueOf(consumerId)).partnerOrderId(kakaoPaymentDto.getPartnerOrderId()).build())
                        .productInfoDtoList(productInfo.getData())
                        .recipientName(paymentCreationDto.getRecipientName())
                        .recipientPhoneNumber(paymentCreationDto.getRecipientPhoneNumber())
                        .basicAddress(paymentCreationDto.getBasicAddress())
                        .addressDetail(paymentCreationDto.getAddressDetail())
                        .zoneCode(paymentCreationDto.getZoneCode())
                        .build())
                .build());
        return exchange;
    }

    public ResponseEntity<String> createCreditInfoWithKakao(MemberCreditChargeDto memberCreditChargeDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi( KAKAO_READY_URL, kakaoPaymentDto.generateKakaoPayApprovePayReady(cid,kakaoPaymentDto.getTotalAmount(),creditApprovalUrl, creditCancelUrl, creditFailUrl ));
        redisUtil.saveRedis(kakaoPaymentDto.getPartnerOrderId(), CreditPaymentDto.builder()
                .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                .chargeCredit(memberCreditChargeDto.getChargeCredit())
                .paymentType(memberCreditChargeDto.getPaymentType())
                .paymentMethod(PaymentMethodEnum.KAKAO)
                .paymentAmount(kakaoPaymentDto.getTotalAmount())
                .paymentTaxFreeAmount(0L)
                .tid(getTargetToken(exchange,"tid"))
                .build());
        return exchange;
    }

    public int callKakaoApproveApi(KakaoPayApproveDto kakaoPayApproveDto) {
        return callKakaoApi(KAKAO_APPROVE_URL, kakaoPayApproveDto.generateKakaoPayApproveData(cid)).getStatusCode().value();
    }

    public ResponseEntity<String> callKakaoSubscriptionApproveApi(KakaoPayApproveDto kakaoPayApproveDto) {
        return callKakaoApi(KAKAO_APPROVE_URL, kakaoPayApproveDto.generateKakaoPayApproveData(subscriptionCid));
    }

    public int callKakaoCancelApi(KakaoPayCancelDto kakaoPayCancelDto) {
        return callKakaoApi(KAKAO_CANCEL_URL, kakaoPayCancelDto.generateKakaoPayCancelData(cid)).getStatusCode().value();
    }

    public String redirectPage(String url, String queryParam) {
        String path = url;
        if(queryParam != null){
            path+="?type=" + queryParam;
        }

        String htmlCode = "<!DOCTYPE html><html><head></head><body>";
        htmlCode += "<script>";
        htmlCode += "window.onload = function() {";
        htmlCode += " window.location.href = '" + path + "';";
        htmlCode += "};";
        htmlCode += "</script>";
        htmlCode += "</body></html>";

        return htmlCode;
    }

    public String getTargetToken(ResponseEntity<String> jsonResponse, String parsingToken){
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.getBody());
            return jsonNode.get(parsingToken).asText();
        } catch (Exception e) {
            throw new RuntimeException("파싱 실패");
        }
    }

    private ResponseEntity<String> callKakaoApi(String url, String payload) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.set("Authorization", "KakaoAK "+kakaoPayKey);

        HttpEntity<String> requestEntity = new HttpEntity<>(payload, httpHeaders);

        return new RestTemplate().exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }

    private void checkValidationCondition(FeignFormat<?> feignFormat){
        if(feignFormat.getFailure()!=null){
            throw new FeignClientResponseException(feignFormat.getFailure());
        }
    }
}