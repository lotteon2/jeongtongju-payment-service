package com.jeontongju.payment.repository;

import com.jeontongju.payment.domain.KakaoPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface KakaoPaymentRepository extends JpaRepository<KakaoPayment, Long> {
    KakaoPayment findByPaymentPaymentId(@Param("paymentId") Long paymentId);
}
