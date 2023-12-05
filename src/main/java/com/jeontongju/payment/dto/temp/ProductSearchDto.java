package com.jeontongju.payment.dto.temp;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ProductSearchDto {
    private Long totalPrice;
    private List<ProductUpdateDto> productUpdateDtoList;
}