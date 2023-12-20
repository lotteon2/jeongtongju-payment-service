package com.jeontongju.payment.dto;

import com.jeontongju.payment.domain.Payment;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 카카오페이 승인 시점에서 비지니스 로직을 수행하기 위해 사용되는 DTO
 * 현재는 크레딧 충전의 경우에만 사용됨
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPaymentDto extends CommonPaymentDto{
    private Long chargeCredit;

    public Payment convertPaymentDtoToPayment(){
        return Payment.builder()
                .consumerId(this.consumerId)
                .paymentType(this.paymentType)
                .paymentMethod(this.paymentMethod)
                .paymentAmount(this.paymentAmount)
                .paymentTaxFreeAmount(0L)
                .build();
    }
}
