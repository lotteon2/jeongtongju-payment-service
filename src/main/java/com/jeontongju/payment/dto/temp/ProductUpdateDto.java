package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 domain : item
 detail : 재고 차감을 위한 DTO임 특정 productId 상품의 재고를 productCount 만큼 감소시키면 됨
 method : kafka
 comment :
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDto {
    private String productId;
    private Long productCount;
}