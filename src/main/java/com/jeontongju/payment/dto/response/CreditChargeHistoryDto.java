package com.jeontongju.payment.dto.response;

import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CreditChargeHistoryDto {
    private Long paymentAmount;
    private LocalDateTime paymentDate;
    private PaymentMethodEnum paymentMethod;
}