package com.jeontongju.payment.dto;

import com.jeontongju.payment.domain.Payment;
import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.enums.temp.PaymentTypeEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * 카카오페이 승인 시점에서 비지니스 로직을 수행하기 위해 사용되는 DTO
 * 현재는 크레딧 충전의 경우에만 사용됨
 */
@Getter
@Builder
public class PaymentDto {
    private Long consumerId;
    private PaymentTypeEnum paymentType;
    private PaymentMethodEnum paymentMethod;
    private Long paymentAmount;
    private Long paymentTaxFreeAmount;
    private String tid;
    private Long chargeCredit;

    public static Payment convertPaymentDtoToPayment(PaymentDto paymentDto){
        return Payment.builder()
                .consumerId(paymentDto.getConsumerId())
                .paymentType(paymentDto.getPaymentType())
                .paymentMethod(paymentDto.getPaymentMethod())
                .paymentAmount(paymentDto.getPaymentAmount())
                .paymentTaxFreeAmount(0L)
                .build();
    }
}
