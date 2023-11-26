package com.jeontongju.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.PaymentDto;
import com.jeontongju.payment.dto.controller.MemberCreditChargeDto;
import com.jeontongju.payment.dto.temp.KakaoPayApproveDto;
import com.jeontongju.payment.dto.temp.KakaoPayCancelDto;
import com.jeontongju.payment.dto.temp.OrderCreationDto;
import com.jeontongju.payment.dto.temp.PaymentCreationDto;
import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.exception.RedisConnectionException;
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

    @Value("${frontUrl}")
    private String frontUrl;

    private final String KAKAO_READY_URL = "https://kapi.kakao.com/v1/payment/ready";

    private final String KAKAO_APPROVE_URL = "https://kapi.kakao.com/v1/payment/approve";

    private final String KAKAO_CANCEL_URL = "https://kapi.kakao.com/v1/payment/cancel";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ResponseEntity<String> createOrderInfoWithKakao(PaymentCreationDto paymentCreationDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, getKakaoPayReadyPayloadData(kakaoPaymentDto,orderApprovalUrl, orderCancelUrl, orderFailUrl));
        saveRedis(kakaoPaymentDto.getPartnerOrderId(), OrderCreationDto.builder().tid(getTid(exchange)).paymentCreationDto(paymentCreationDto).consumerId(kakaoPaymentDto.getPartnerUserId()).build());
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

//    public String generatePageRedirectionCode(String url) {
//        String path = frontUrl + url;
//
//        String htmlCode = "<!DOCTYPE html><html><head></head><body>";
//        htmlCode += "<script>";
//        htmlCode += "window.onload = function() {";
//        htmlCode += " window.location.href = '" + path + "';";
//        htmlCode += "};";
//        htmlCode += "</script>";
//        htmlCode += "</body></html>";
//
//        return htmlCode;
//    }
}