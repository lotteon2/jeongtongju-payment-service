package com.jeontongju.payment.dto.temp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPayMethod implements PaymentMethod {
    private String tid;
    private String pgToken;

    public void setPgToken(String pgToken) {
        this.pgToken = pgToken;
    }
}
