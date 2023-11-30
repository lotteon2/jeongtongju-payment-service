package com.jeontongju.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.jeontongju.payment.exception.RedisConnectionException;
import com.jeontongju.payment.feign.ProductFeignServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
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

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductFeignServiceClient productFeignServiceClient;

    public ResponseEntity<String> createOrderInfoWithKakao(PaymentCreationDto paymentCreationDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, getKakaoPayReadyPayloadData(kakaoPaymentDto,orderApprovalUrl, orderCancelUrl, orderFailUrl));

        // ProductUpdateDto 만드는 표현식(Feign 및 재고차감 하라는 요청을 보낼때 사용)
        List<ProductUpdateDto> productSearchDtoList = paymentCreationDto.getProducts().stream()
                .map(productDto -> ProductUpdateDto.builder()
                        .productId(productDto.getProductId())
                        .productCount(productDto.getProductCount())
                        .build())
                .collect(Collectors.toList());

        // Feign을 요청하고 Feign이 200이 아니라면 예외 리턴
        FeignFormat<List<ProductInfoDto>> productInfo = productFeignServiceClient.getProductInfo(ProductSearchDto.builder()
                .productUpdateDtoList(productSearchDtoList).totalPrice(paymentCreationDto.getTotalAmount()).build());
        if(productInfo.getCode() != 200){
            throw new KakaoPayException("카카오페이 QR 코드를 만드는데 실패했습니다.");
        }

        Long consumerId = Long.valueOf(kakaoPaymentDto.getPartnerUserId());
        saveRedis(kakaoPaymentDto.getPartnerOrderId(), OrderInfoDto.builder()
                .userPointUpdateDto(UserPointUpdateDto.builder().consumerId(consumerId).point(paymentCreationDto.getPointUsageAmount()).build())
                .userCouponUpdateDto(UserCouponUpdateDto.builder().consumerId(consumerId).couponCode(paymentCreationDto.getCouponCode())
                        .couponAmount(paymentCreationDto.getCouponAmount()).build())
                .productUpdateDto(productSearchDtoList)
                .orderCreationDto(OrderCreationDto.builder()
                        .totalPrice(paymentCreationDto.getTotalAmount())
                        .consumerId(consumerId)
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
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, getKakaoPayReadyPayloadData(kakaoPaymentDto, creditApprovalUrl, creditCancelUrl, creditFailUrl));
        saveRedis(kakaoPaymentDto.getPartnerOrderId(), PaymentDto.builder()
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
        return callKakaoApi(KAKAO_APPROVE_URL, getKakaoPayApprovePayLoadData(kakaoPayApproveDto)).getStatusCode().value();
    }

    public int callKakaoCancelApi(KakaoPayCancelDto kakaoPayCancelDto) {
        return callKakaoApi(KAKAO_CANCEL_URL, getKakaoPayCancelPayLoadData(kakaoPayCancelDto)).getStatusCode().value();
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

    private String getKakaoPayReadyPayloadData(KakaoPaymentDto kakaoPaymentDto, String approvalUrl, String cancelUrl, String failUrl){
        return "cid=" + cid
                + "&partner_order_id=" + kakaoPaymentDto.getPartnerOrderId()
                + "&partner_user_id=" + kakaoPaymentDto.getPartnerUserId()
                + "&item_name=" + URLEncoder.encode(kakaoPaymentDto.getItemName())
                + "&quantity=" + kakaoPaymentDto.getQuantity()
                + "&total_amount=" + kakaoPaymentDto.getTotalAmount()
                + "&tax_free_amount=" + kakaoPaymentDto.getTaxFreeAmount()
                + "&approval_url=" + approvalUrl + "?partnerOrderId=" + kakaoPaymentDto.getPartnerOrderId()
                + "&cancel_url=" + cancelUrl
                + "&fail_url=" + failUrl;
    }

    private String getKakaoPayApprovePayLoadData(KakaoPayApproveDto kakaoPayApproveDto){
        return "cid=" + cid
                + "&tid=" + kakaoPayApproveDto.getTid()
                + "&partner_order_id=" + kakaoPayApproveDto.getPartnerOrderId()
                + "&partner_user_id=" + kakaoPayApproveDto.getPartnerUserId()
                + "&pg_token=" + kakaoPayApproveDto.getPgToken();
    }


    private String getKakaoPayCancelPayLoadData(KakaoPayCancelDto kakaoPayCancelDto){
        return "cid=" + cid
                + "&tid=" + kakaoPayCancelDto.getTid()
                + "&cancel_amount=" + kakaoPayCancelDto.getCancelAmount()
                + "&cancel_tax_free_amount=" + kakaoPayCancelDto.getCancelTaxFreeAmount();
    }

    private String getTid(ResponseEntity<String> jsonResponse){
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.getBody());
            return jsonNode.get("tid").asText();
        } catch (Exception e) {
            throw new RuntimeException("파싱 실패");
        }
    }

    private void saveRedis(String orderId, Object saveData){
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        try {
            vop.set(orderId, objectMapper.writeValueAsString(saveData), Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RedisConnectionException("레디스에 들어갈 데이터를 만드는 과정에서 문제가 발생했습니다.");
        }
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
}