package com.jeontongju.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 카카오 페이 호출시 사용되는 DTO
 */
@Getter
@Builder
public class KakaoPaymentDto {
    private String partnerUserId;
    private String partnerOrderId;
    private String itemName;
    private Long quantity;
    private Long totalAmount;
    private Long taxFreeAmount;

    public static KakaoPaymentDto convertPaymentDto(String userId, String titleName, Long totalAmount){
        return KakaoPaymentDto.builder()
                .partnerUserId(userId)
                .partnerOrderId(UUID.randomUUID().toString())
                .itemName(titleName)
                .quantity(1L)
                .totalAmount(totalAmount)
                .taxFreeAmount(totalAmount/10)
                .build();
    }
}