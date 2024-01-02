package com.jeontongju.payment.domain;

import com.jeontongju.payment.domain.common.BaseEntity;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.Column;
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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
@DynamicInsert
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @NotNull
    private Long consumerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum paymentMethod;

    @NotNull
    private Long paymentAmount;

    @NotNull
    private Long paymentTaxFreeAmount;

    @NotNull
    @Column(columnDefinition = "boolean default true")
    private boolean isDeleted;

    @OneToOne(mappedBy = "payment")
    private KakaoPayment kakaoPayment;
}