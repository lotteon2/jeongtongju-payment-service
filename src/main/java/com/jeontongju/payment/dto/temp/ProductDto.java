package com.jeontongju.payment.dto.temp;

import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
public class ProductDto {
    @NotNull(message = "상품코드는 필수 입니다.")
    private String productId;
    @NotNull(message = "상품수량은 필수 입니다.")
    private Long productCount;
}
