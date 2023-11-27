package com.jeontongju.payment.dto.temp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 domain : payment, order
 detail : 상품정보를 담고 있는 DTO
 method :
 comment : 주문정보에서 해당 상품정보를 가지고 있어야 하기 때문에 해당 DTO가 존재함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ProductDto {
    @NotEmpty(message = "상품코드는 필수 입니다.")
    private String productId;
    @NotNull(message = "상품수량은 필수 입니다.")
    private Long productCount;
}
