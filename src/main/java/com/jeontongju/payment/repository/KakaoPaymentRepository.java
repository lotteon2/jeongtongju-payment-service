package com.jeontongju.payment.repository;

import com.jeontongju.payment.domain.KakaoPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoPaymentRepository extends JpaRepository<KakaoPayment, Long> {
    KakaoPayment findByPaymentPaymentId(Long paymentId);
}
