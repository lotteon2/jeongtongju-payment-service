package com.jeontongju.payment.feign;

import io.github.bitbox.bitbox.dto.FeignFormat;
import io.github.bitbox.bitbox.dto.UserPointUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="consumer-service")
public interface PointFeignServiceClient {
    // 해당 사용자가 해당 포인트만큼 포인트를 가지고 있는지 체크하는 Feign
    @PostMapping("/point")
    FeignFormat<Boolean> checkConsumerPoint(@RequestBody UserPointUpdateDto userPointUpdateDto);
}
