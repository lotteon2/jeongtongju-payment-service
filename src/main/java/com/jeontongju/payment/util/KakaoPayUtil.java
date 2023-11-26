package com.jeontongju.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.dto.KakaoPaymentDto;
import com.jeontongju.payment.dto.temp.OrderCreationDto;
import com.jeontongju.payment.dto.temp.PaymentCreationDto;
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

    @Value("${approvalUrl}")
    private String approvalUrl;

    @Value("${cancelUrl}")
    private String cancelUrl;

    @Value("${failUrl}")
    private String failUrl;

    @Value("${kakaoPayKey}")
    private String kakaoPayKey;

    @Value("${frontUrl}")
    private String frontUrl;

    private final String KAKAO_READY_URL = "https://kapi.kakao.com/v1/payment/ready";

    private final String KAKAO_APPROVE_URL = "https://kapi.kakao.com/v1/payment/approve";

    private final String KAKAO_CANCEL_URL = "https://kapi.kakao.com/v1/payment/cancel";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ResponseEntity<String> callKakaoReadyApi(PaymentCreationDto paymentCreationDto, KakaoPaymentDto kakaoPaymentDto) {
        ResponseEntity<String> exchange = callKakaoApi(KAKAO_READY_URL, getKakaoPayReadyPayloadData(kakaoPaymentDto));
        savePaymentInfoToRedis(exchange, paymentCreationDto, kakaoPaymentDto);
        return exchange;
    }

    /*public int callKakaoApproveApi(KakaoPayDto kakaoPayDto) {
        try{
            return callKakaoApi(kakaoApproveUrl, getKakaoPayApprovePayLoadData(kakaoPayDto)).getStatusCode().value();
        }catch(KakaoPayFailException e){
            throw new KakaoPayFailException("카카오 페이 결제 실패");
        }
    }

    public int callKakaoCancelApi(KakaoPayCancelDto kakaoPayCancelDto) {
        try {
            return callKakaoApi(kakaoCancelUrl, getKakaoPayCancelPayLoadData(kakaoPayCancelDto)).getStatusCode().value();
        }catch(KakaoPayFailException e){
            log.error("에러 발생"); // TODO 이런거 로그파일로 관리해야함
            throw new UrgentMailException("카카오페이 취소하는 과정에서 에러발생",e);
        }
    }*/

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


    /*public KakaoPayDto getKakaoPayDto(String inputString, String pgToken) {
        try {
            KakaoPayDto kakaoPayDto = objectMapper.readValue(inputString, KakaoPayDto.class);
            kakaoPayDto.setPgToken(pgToken);
            return kakaoPayDto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오페이 관련 값을 만드는데 실패했습니다.");
        }
    }*/

    private String getKakaoPayReadyPayloadData(KakaoPaymentDto kakaoPaymentDto){
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

    /*private String getKakaoPayApprovePayLoadData(KakaoPayDto kakaoPayDto){
        return "cid=" + kakaoPayDto.getCid()
                + "&tid=" + kakaoPayDto.getTid()
                + "&partner_order_id=" + kakaoPayDto.getPartnerOrderId()
                + "&partner_user_id=" + kakaoPayDto.getPartnerUserId()
                + "&pg_token=" + kakaoPayDto.getPgToken();
    }

    private String getKakaoPayCancelPayLoadData(KakaoPayCancelDto kakaoPayCancelDto){
        return "cid=" + cid
                + "&tid=" + kakaoPayCancelDto.getTid()
                + "&cancel_amount=" + kakaoPayCancelDto.getCancelAmount()
                + "&cancel_tax_free_amount=" + kakaoPayCancelDto.getCancelTaxFreeAmount();
    }*/

    private void savePaymentInfoToRedis(ResponseEntity<String> jsonResponse, PaymentCreationDto paymentCreationDto, KakaoPaymentDto paymentDto){
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        String tid;

        try {
            tid = objectMapper.readTree(jsonResponse.getBody()).get("tid").asText();
        } catch (Exception e) {
            throw new RuntimeException("파싱 실패");
        }
        OrderCreationDto orderCreationDto = OrderCreationDto.builder().tid(tid).paymentCreationDto(paymentCreationDto).build();

        try {
            vop.set(paymentDto.getPartnerOrderId(), objectMapper.writeValueAsString(orderCreationDto), Duration.ofMinutes(5));
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