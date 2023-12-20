package com.jeontongju.payment.dto;

import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import io.github.bitbox.bitbox.enums.SubscriptionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDto {
    @NotNull
    private PaymentTypeEnum paymentType;
    @NotNull
    private PaymentMethodEnum paymentMethod;
    @NotNull
    private String itemName;
    @NotNull
    private SubscriptionTypeEnum subscriptionType;
}
