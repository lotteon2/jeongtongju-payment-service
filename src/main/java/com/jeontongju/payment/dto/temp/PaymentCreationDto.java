package com.jeontongju.payment.dto.temp;

import com.jeontongju.payment.enums.PaymentMethodEnum;
import com.jeontongju.payment.enums.PaymentTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCreationDto {
    @NotNull(message = "결제종류는 필수 입니다.")
    private PaymentTypeEnum paymentType;

    @NotNull(message = "결제타입은 필수 입니다.")
    private PaymentMethodEnum paymentMethod;

    @Range(min=0, message = "사용할 포인트는 최소 0 이상 입니다.")
    private Long pointUsageAmount;

    private String couponCode;

    @NotNull(message = "상품 이미지는 필수 입니다.")
    private String productImg;

    @NotNull(message = "받는 사람 이름은 필수 입니다.")
    private String recipientName;

    @NotNull(message = "받는사람 휴대폰 번호는 필수 입니다.")
    private String recipientPhoneNumber;

    @NotNull(message = "주소는 필수 입니다.")
    private String basicAddress;

    private String addressDetail;

    @NotNull(message = "우편번호는 필수 입니다.")
    private String zoneCode;

    @NotNull(message = "결제금액은 필수 입니다.")
    private Long totalAmount;

    @NotNull(message = "결제명은 필수 입니다.")
    private String titleName;

    @Valid
    @NotNull(message = "상품목록은 필수 입니다.")
    private List<ProductDto> products;
}