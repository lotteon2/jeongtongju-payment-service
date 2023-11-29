package com.jeontongju.payment.repository;

import com.jeontongju.payment.domain.Payment;
import com.jeontongju.payment.dto.response.CreditChargeHistoryDto;
import com.jeontongju.payment.enums.temp.PaymentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT new com.jeontongju.payment.dto.response.CreditChargeHistoryDto(p.paymentAmount, p.createdAt, p.paymentMethod) " +
            "FROM Payment p " +
            "WHERE p.consumerId = :consumerId AND p.paymentType = :paymentType " +
            "ORDER BY p.createdAt DESC")
    Page<CreditChargeHistoryDto> findCreditChargeHistoryByConsumerIdAndPaymentType(
            @Param("consumerId") Long consumerId,
            @Param("paymentType") PaymentTypeEnum paymentType,
            Pageable pageable);

}