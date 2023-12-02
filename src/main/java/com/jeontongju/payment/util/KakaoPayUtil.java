package com.jeontongju.payment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.MemberCreditChargeDto;
import com.jeontongju.payment.dto.PaymentCreationDto;
import com.jeontongju.payment.dto.PaymentDto;
import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.KakaoPayApproveDto;
import com.jeontongju.payment.dto.temp.KakaoPayCancelDto;
import com.jeontongju.payment.dto.temp.OrderCreationDto;
import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.dto.temp.ProductInfoDto;
import com.jeontongju.payment.dto.temp.ProductSearchDto;
import com.jeontongju.payment.dto.temp.ProductUpdateDto;
import com.jeontongju.payment.dto.temp.UserCouponUpdateDto;
import com.jeontongju.payment.dto.temp.UserPointUpdateDto;
import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.enums.temp.PaymentTypeEnum;
import com.jeontongju.payment.exception.KakaoPayException;
import com.jeontongju.payment.feign.CouponFeignServiceClient;
import com.jeontongju.payment.feign.PointFeignServiceClient;
import com.jeontongju.payment.feign.ProductFeignServiceClient;
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

    public ResponseEntity<String> createOrderInfoWithKakao(PaymentCreationDto paymentCreationDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, kakaoPaymentDto.generateKakaoPayApprovePayReady(cid,orderApprovalUrl, orderCancelUrl, orderFailUrl ));

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
        if(productInfo.getCode() != 200){
            throw new KakaoPayException("카카오페이 QR 코드를 만드는데 실패했습니다.");
        }

        if(paymentCreationDto.getPointUsageAmount()!=null) { // optional
            // Consumer에 Feign을 요청한다. (포인트가 부족한 경우는 예외 발생한다)
            FeignFormat<Boolean> pointInfo = pointFeignServiceClient.checkConsumerPoint(UserPointUpdateDto.builder()
                    .point(paymentCreationDto.getPointUsageAmount())
                    .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                    .build());
            if (pointInfo.getCode() != 200) {
                throw new KakaoPayException("카카오페이 QR 코드를 만드는데 실패했습니다.");
            }
        }

        if(paymentCreationDto.getCouponCode()!=null) { // optional
            // coupon에 Feign을 요청한다. (쿠폰을 사용할 수 없는 경우는 예외 발생한다)
            FeignFormat<Void> couponInfo = couponFeignServiceClient.checkCouponInfo(UserCouponUpdateDto.builder()
                    .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                    .couponCode(paymentCreationDto.getCouponCode())
                    .couponAmount(paymentCreationDto.getCouponAmount())
                    .totalAmount(paymentCreationDto.getTotalAmount())
                    .build());
            if (couponInfo.getCode() != 200) {
                throw new KakaoPayException("카카오페이 QR 코드를 만드는데 실패했습니다.");
            }
        }

        Long consumerId = Long.valueOf(kakaoPaymentDto.getPartnerUserId());
        redisUtil.saveRedis(kakaoPaymentDto.getPartnerOrderId(), OrderInfoDto.builder()
                .userPointUpdateDto(UserPointUpdateDto.builder().consumerId(consumerId).point(paymentCreationDto.getPointUsageAmount()).build())
                .userCouponUpdateDto(UserCouponUpdateDto.builder().consumerId(consumerId).couponCode(paymentCreationDto.getCouponCode())
                        .couponAmount(paymentCreationDto.getCouponAmount()).build())
                        .productUpdateDto(productSearchDtoList)
                        .orderCreationDto(OrderCreationDto.builder()
                        .totalPrice(paymentCreationDto.getTotalAmount())
                        .consumerId(consumerId)
                        .orderDate(LocalDateTime.now())
                        .orderId(kakaoPaymentDto.getPartnerOrderId())
                        .paymentType(PaymentTypeEnum.ORDER)
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
        ResponseEntity<String> exchange = callKakaoApi( KAKAO_READY_URL, kakaoPaymentDto.generateKakaoPayApprovePayReady(cid,creditApprovalUrl, creditCancelUrl, creditFailUrl ));
        redisUtil.saveRedis(kakaoPaymentDto.getPartnerOrderId(), PaymentDto.builder()
                .consumerId(Long.valueOf(kakaoPaymentDto.getPartnerUserId()))
                .chargeCredit(memberCreditChargeDto.getChargeCredit())
                .paymentType(memberCreditChargeDto.getPaymentType())
                .paymentMethod(PaymentMethodEnum.KAKAO)
                .paymentAmount(kakaoPaymentDto.getTotalAmount())
                .paymentTaxFreeAmount(kakaoPaymentDto.getTaxFreeAmount())
                .tid(getTid(exchange))
                .build());
        return exchange;
    }

    public int callKakaoApproveApi(KakaoPayApproveDto kakaoPayApproveDto) {
        return callKakaoApi(KAKAO_APPROVE_URL, kakaoPayApproveDto.generateKakaoPayApproveData(cid)).getStatusCode().value();
    }

    public int callKakaoCancelApi(KakaoPayCancelDto kakaoPayCancelDto) {
        return callKakaoApi(KAKAO_CANCEL_URL, kakaoPayCancelDto.generateKakaoPayCancelData(cid)).getStatusCode().value();
    }

    public String generateFailPage() {
        String htmlCode = "<!DOCTYPE html><html><head></head><body>";
        htmlCode += "<h1>문제가 발생했습니다. 해당 창을 끄고 다시 시도해주세요 </h1>";
        htmlCode += "</body></html>";

        return htmlCode;
    }

    public String generatePageCloseCodeWithAlert(String alertMessage) {
        String htmlCode = "<!DOCTYPE html><html><head></head><body>";
        htmlCode += "<script>";
        htmlCode += "window.onload = function() {";
        if(alertMessage != null) {
            htmlCode += "  alert('" + alertMessage + "');";
        }
        htmlCode += "  window.close();";
        htmlCode += "};";
        htmlCode += "</script>";
        htmlCode += "</body></html>";

        return htmlCode;
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

    private String getTid(ResponseEntity<String> jsonResponse){
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.getBody());
            return jsonNode.get("tid").asText();
        } catch (Exception e) {
            throw new RuntimeException("파싱 실패");
        }
    }
}