package com.jeontongju.payment.service;

import com.jeontongju.payment.domain.KakaoPayment;
import com.jeontongju.payment.domain.Payment;
import com.jeontongju.payment.domain.PaymentOrder;
import com.jeontongju.payment.dto.PaymentDto;
import com.jeontongju.payment.dto.response.CreditChargeHistoryDto;
import com.jeontongju.payment.dto.temp.CreditUpdateDto;
import com.jeontongju.payment.dto.temp.KakaoPayApproveDto;
import com.jeontongju.payment.dto.temp.KakaoPayCancelDto;
import com.jeontongju.payment.dto.temp.KakaoPayMethod;
import com.jeontongju.payment.dto.temp.OrderInfoDto;
import com.jeontongju.payment.enums.temp.PaymentMethodEnum;
import com.jeontongju.payment.enums.temp.PaymentTypeEnum;
import com.jeontongju.payment.exception.KakaoPayApproveException;
import com.jeontongju.payment.repository.KakaoPaymentRepository;
import com.jeontongju.payment.repository.PaymentOrderRepository;
import com.jeontongju.payment.repository.PaymentRepository;
import com.jeontongju.payment.util.KakaoPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final KakaoPayUtil kakaoPayUtil;
    private final KafkaTemplate<String, CreditUpdateDto> kafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final KakaoPaymentRepository kakaoPaymentRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final String UPDATE_CREDIT_TOPIC = "update-credit";

    @Transactional
    public void createPayment(String partnerOrderId, String pgToken, PaymentDto paymentDto) {
        Payment payment = paymentRepository.save(PaymentDto.convertPaymentDtoToPayment(paymentDto));
        kakaoPaymentRepository.save(KakaoPayment.builder().payment(payment).tid(paymentDto.getTid()).build());
        if (kakaoPayUtil.callKakaoApproveApi(KakaoPayApproveDto.builder()
                        .partnerOrderId(partnerOrderId)
                        .tid(paymentDto.getTid())
                        .partnerUserId(String.valueOf(paymentDto.getConsumerId()))
                        .pgToken(pgToken)
                        .build())
                != HttpStatus.SC_OK) {
            throw new KakaoPayApproveException("카카오 페이 승인 실패");
        }

        try {
            kafkaTemplate.send(UPDATE_CREDIT_TOPIC, CreditUpdateDto.builder()
                    .consumerId(paymentDto.getConsumerId())
                    .credit(paymentDto.getChargeCredit())
                    .tid(paymentDto.getTid())
                    .cancelAmount(paymentDto.getPaymentAmount())
                    .cancelTaxFreeAmount(paymentDto.getPaymentTaxFreeAmount())
                    .build());
        }catch(Exception e){
            kakaoPayUtil.callKakaoCancelApi(KakaoPayCancelDto.builder()
                    .tid(paymentDto.getTid())
                    .cancelAmount(paymentDto.getPaymentAmount())
                    .cancelTaxFreeAmount(paymentDto.getPaymentTaxFreeAmount())
                    .build());
            throw new KafkaException("카프카 예외 발생");
        }
    }

    @Transactional
    public void createOrderPaymentInfo(OrderInfoDto orderInfoDto){
        long totalPrice = orderInfoDto.getOrderCreationDto().getTotalPrice();
        long point = orderInfoDto.getUserPointUpdateDto().getPoint() != null ? orderInfoDto.getUserPointUpdateDto().getPoint() : 0L;
        long couponAmount = orderInfoDto.getUserCouponUpdateDto().getCouponAmount() != null ? orderInfoDto.getUserCouponUpdateDto().getCouponAmount() : 0L;
        long realPrice = totalPrice - point - couponAmount;

        // payment 테이블 생성
        Payment payment = Payment.builder()
                .consumerId(orderInfoDto.getOrderCreationDto().getConsumerId())
                .paymentType(PaymentTypeEnum.ORDER)
                .paymentMethod(orderInfoDto.getOrderCreationDto().getPaymentMethod())
                .paymentAmount(realPrice)
                .paymentTaxFreeAmount(realPrice/10)
        .build();
        paymentRepository.save(payment);

        // paymentOrder 테이블 생성
        paymentOrderRepository.save(PaymentOrder.builder()
                .payment(payment)
                .ordersId(orderInfoDto.getOrderCreationDto().getOrderId())
                .totalPrice(totalPrice)
                .minusCouponAmount(couponAmount)
                .minusPointAmount(point)
                .couponCode(orderInfoDto.getUserCouponUpdateDto().getCouponCode())
        .build());

        // kakao_payment 테이블 생성
        if(orderInfoDto.getOrderCreationDto().getPaymentMethod() == PaymentMethodEnum.KAKAO){
            KakaoPayMethod kakaoPayMethod = (KakaoPayMethod) orderInfoDto.getOrderCreationDto().getPaymentInfo();

            kakaoPaymentRepository.save(KakaoPayment.builder()
                    .payment(payment)
                    .tid(kakaoPayMethod.getTid())
                    .build()
            );

            kakaoPayUtil.callKakaoApproveApi(KakaoPayApproveDto.builder()
                    .tid(kakaoPayMethod.getTid())
                    .pgToken(kakaoPayMethod.getPgToken())
                    .partnerOrderId(kakaoPayMethod.getPartnerOrderId())
                    .partnerUserId(kakaoPayMethod.getPartnerUserId())
            .build());
        }
    }

    public Page<CreditChargeHistoryDto> getConsumerCreditHistory(Long consumerId, Pageable pageable){
        return paymentRepository.findCreditChargeHistoryByConsumerIdAndPaymentType(consumerId, PaymentTypeEnum.CREDIT, pageable);
    }
}
