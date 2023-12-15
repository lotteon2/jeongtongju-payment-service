package com.jeontongju.payment.repository;

import com.jeontongju.payment.domain.PaymentOrder;
import io.github.bitbox.bitbox.dto.PaymentInfoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    @Query("SELECT new io.github.bitbox.bitbox.dto.PaymentInfoDto(p.minusPointAmount, p.minusCouponAmount, p.couponCode, p.totalPrice, p.totalPrice - p.minusCouponAmount - p.minusPointAmount) FROM PaymentOrder p WHERE p.ordersId = :ordersId")
    PaymentInfoDto findByOrdersIdWithDto(String ordersId);
    PaymentOrder findByOrdersId(String ordersId);
}
