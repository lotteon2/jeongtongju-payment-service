package com.jeontongju.payment.repository;

import com.jeontongju.payment.domain.PaymentOrder;
import com.jeontongju.payment.dto.temp.PaymentInfoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    @Query("SELECT new com.jeontongju.payment.dto.temp.PaymentInfoDto(p.minusPointAmount, p.minusCouponAmount, p.couponCode) FROM PaymentOrder p WHERE p.ordersId = :ordersId")
    PaymentInfoDto findByOrdersIdWithDto(@Param("ordersId") String ordersId);
    PaymentOrder findByOrdersId(@Param("ordersId") String ordersId);
}
