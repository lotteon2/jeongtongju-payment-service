package com.jeontongju.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.net.URLEncoder;
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

    public String generateKakaoPayApprovePayReady(String cid, String approvalUrl, String cancelUrl, String failUrl){
        return "cid=" + cid
                + "&partner_order_id=" + this.getPartnerOrderId()
                + "&partner_user_id=" + this.getPartnerUserId()
                + "&item_name=" + URLEncoder.encode(this.getItemName())
                + "&quantity=" + this.getQuantity()
                + "&total_amount=" + this.getTotalAmount()
                + "&tax_free_amount=" + this.getTaxFreeAmount()
                + "&approval_url=" + approvalUrl + "?partnerOrderId=" + this.getPartnerOrderId()
                + "&cancel_url=" + cancelUrl
                + "&fail_url=" + failUrl;
    }
}