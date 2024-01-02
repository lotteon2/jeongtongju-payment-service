package com.jeontongju.payment.dto;

import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommonPaymentDto {
    protected Long consumerId;
    protected PaymentTypeEnum paymentType;
    protected Long paymentAmount;
    protected PaymentMethodEnum paymentMethod;
    protected Long paymentTaxFreeAmount;
    protected String tid;
}
