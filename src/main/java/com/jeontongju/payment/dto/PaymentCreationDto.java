package com.jeontongju.payment.dto;

import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 domain : payment, order
 detail : 결제정보를 담고 있는 DTO
 method :
 comment : 주문정보에서 해당 결제정보를 가지고 있어야 하기 때문에 해당 DTO가 존재함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreationDto {
    @NotNull(message = "결제종류는 필수 입니다.")
    private PaymentTypeEnum paymentType;

    @NotNull(message = "결제타입은 필수 입니다.")
    private PaymentMethodEnum paymentMethod;

    @Min(value = 0, message = "사용할 포인트는 최소 0 이상 입니다.")
    private Long pointUsageAmount;

    private String couponCode;

    private Long couponAmount;

    @NotEmpty(message = "받는 사람 이름은 필수 입니다.")
    private String recipientName;

    @NotEmpty(message = "받는사람 휴대폰 번호는 필수 입니다.")
    private String recipientPhoneNumber;

    @NotEmpty(message = "주소는 필수 입니다.")
    private String basicAddress;

    private String addressDetail;

    @NotEmpty(message = "우편번호는 필수 입니다.")
    private String zoneCode;

    @NotNull(message = "결제금액은 필수 입니다.")
    private Long totalAmount;

    @NotNull(message = "실결제금액은 필수 입니다.")
    private Long realAmount;

    @NotEmpty(message = "결제명은 필수 입니다.")
    private String titleName;

    @NotNull(message = "장바구니 주문여부는 필수 입니다.")
    private Boolean isCart;

    @Valid
    @NotNull(message = "상품목록은 필수 입니다.")
    private List<ProductDto> products;
}