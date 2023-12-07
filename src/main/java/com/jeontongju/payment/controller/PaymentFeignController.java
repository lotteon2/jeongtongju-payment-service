package com.jeontongju.payment.controller;

import com.jeontongju.payment.dto.temp.FeignFormat;
import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.dto.temp.PaymentInfoDto;
import com.jeontongju.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("payment-info")
    public FeignFormat<PaymentInfoDto> getPaymentInfo(@RequestParam String orderId) {
        return FeignFormat.<PaymentInfoDto>builder()
                .code(HttpStatus.SC_OK)
                .data(paymentService.getPaymentInfo(orderId))
        .build();
    }
}