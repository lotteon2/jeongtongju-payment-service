package com.jeontongju.payment.controller;

import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/")
@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentFeignController {
    private final PaymentService paymentService;

    @PostMapping("pay-approve")
    public FeignFormat<Void> approveKakaopay(@RequestBody OrderInfoDto orderInfoDto) {
        paymentService.createOrderPaymentInfo(orderInfoDto);

        return FeignFormat.<Void>builder()
                .code(HttpStatus.SC_OK)
                .build();
    }
}