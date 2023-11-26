package com.jeontongju.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PaymentTypeEnum {
    CREDIT,
    ORDER,
    SUBSCRIPTION
}
