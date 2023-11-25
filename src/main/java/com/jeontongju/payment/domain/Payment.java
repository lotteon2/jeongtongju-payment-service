package com.jeontongju.payment.domain;

import com.jeontongju.payment.domain.common.BaseEntity;
import com.jeontongju.payment.enums.PaymentMethod;
import com.jeontongju.payment.enums.PaymentType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @NotNull
    private Long consumerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @NotNull
    private Long paymentAmount;

    @NotNull
    private Long paymentTaxFreeAmount;

    @OneToOne(mappedBy = "payment")
    private KakaoPayment kakaoPayment;
}