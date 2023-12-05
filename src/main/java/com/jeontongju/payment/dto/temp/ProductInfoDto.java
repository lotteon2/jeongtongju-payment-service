package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 domain : product, payment, order
 detail : 주문하려는 상품에 대한 정보를 담고 있는 DTO(리턴)
 method : Feign
 comment : Feign으로 특정 상품 요청시 아래와 같은 format으로 줘야함(productCount는 보낸 개수 그대로 리턴)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfoDto {
    private String productId;
    private String productName;
    private Long productPrice;
    private Long productCount;
    private Long sellerId;
    private String sellerName;
    private String productImg;
}