package com.jeontongju.payment.dto.controller;

import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.enums.temp.PaymentTypeEnum;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 크레딧 충전을 위해 입력받는 dto
*/
@Getter
@Builder
public class MemberCreditChargeDto {
    @Min(value = 1, message = "최소 크레딧은 1이상입니다.")
    private Long chargeCredit;
    @NotNull(message = "결제타입은 비어있을 수 없습니다.")
    private PaymentTypeEnum paymentType;
    @NotNull(message = "구매하는 아이템이름은 비어있을 수 없습니다.")
    private String itemName;
    @NotNull(message = "결제타입은 필수 입니다.")
    private PaymentMethodEnum paymentMethod;
}
