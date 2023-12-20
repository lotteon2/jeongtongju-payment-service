package com.jeontongju.payment.dto;

import com.jeontongju.payment.domain.Payment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class SubscriptionPaymentDto extends CommonPaymentDto{
    public Payment convertSubscriptionPaymentDtoToPayment(){
        return Payment.builder()
                .consumerId(this.consumerId)
                .paymentType(this.paymentType)
                .paymentMethod(this.paymentMethod)
                .paymentAmount(this.paymentAmount)
                .paymentTaxFreeAmount(0L)
                .build();
    }
}