package com.jeontongju.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PaymentType {
    CREDIT,
    ORDER,
    SUBSCRIPTION
}
