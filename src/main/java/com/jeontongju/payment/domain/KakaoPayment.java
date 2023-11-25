package com.jeontongju.payment.domain;

import com.jeontongju.payment.domain.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class KakaoPayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kakaoPaymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @NotNull
    private Payment payment;

    @NotNull
    private String tid;
}