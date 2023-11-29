package com.jeontongju.payment.feign;

import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.ProductInfoDto;
import com.jeontongju.payment.dto.temp.ProductSearchDto;
import com.jeontongju.payment.dto.temp.ProductUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="product-service")
public interface ProductFeignServiceClient {
    // 해당 Feign에서는 프론트가 넘겨준 총가격과 상품들의 총가격을 비교하여 틀리면 예외를 반환해줘야함
    @PostMapping("/products")
    FeignFormat<List<ProductInfoDto>> getProductInfo(@RequestBody ProductSearchDto productSearchDto);
}
