package com.jeontongju.payment.service;

import com.jeontongju.payment.domain.KakaoPayment;
import com.jeontongju.payment.domain.Payment;
import com.jeontongju.payment.domain.PaymentOrder;
import com.jeontongju.payment.dto.CreditPaymentDto;
import com.jeontongju.payment.dto.SubscriptionPaymentDto;
import com.jeontongju.payment.dto.response.CreditChargeHistoryDto;
import com.jeontongju.payment.exception.KakaoPayApproveException;
import com.jeontongju.payment.kafka.KafkaProcessor;
import com.jeontongju.payment.repository.KakaoPaymentRepository;
import com.jeontongju.payment.repository.PaymentOrderRepository;
import com.jeontongju.payment.repository.PaymentRepository;
import com.jeontongju.payment.util.KakaoPayUtil;
import io.github.bitbox.bitbox.dto.CreditUpdateDto;
import io.github.bitbox.bitbox.dto.KakaoPayApproveDto;
import io.github.bitbox.bitbox.dto.KakaoPayCancelDto;
import io.github.bitbox.bitbox.dto.KakaoPayMethod;
import io.github.bitbox.bitbox.dto.KakaoSubscription;
import io.github.bitbox.bitbox.dto.MemberInfoForNotificationDto;
import io.github.bitbox.bitbox.dto.OrderCancelDto;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.dto.PaymentInfoDto;
import io.github.bitbox.bitbox.dto.ProductUpdateListDto;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.dto.SubscriptionDto;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import io.github.bitbox.bitbox.enums.SubscriptionTypeEnum;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final KakaoPayUtil kakaoPayUtil;
    private final KafkaTemplate<String, CreditUpdateDto> creditUpdateDtoKafkaTemplate;
    private final KafkaTemplate<String, SubscriptionDto> subscriptionDtoKafkaTemplate;
    private final KafkaTemplate<String, ProductUpdateListDto> productUpdateDtoKafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final KakaoPaymentRepository kakaoPaymentRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final KafkaProcessor<MemberInfoForNotificationDto> memberInfoForNotificationDtoKafkaProcessor;
    @Value("${subscriptionCid}")
    private String subscriptionCid;

    @Transactional
    public void createSubscription(String partnerOrderId, String pgToken, SubscriptionPaymentDto subscriptionPaymentDto) {
        Payment payment = paymentRepository.save(subscriptionPaymentDto.convertSubscriptionPaymentDtoToPayment());

        kakaoPaymentRepository.save(KakaoPayment.builder().payment(payment).tid(subscriptionPaymentDto.getTid()).build());
        ResponseEntity<String> response = kakaoPayUtil.callKakaoSubscriptionApproveApi(KakaoPayApproveDto.builder().partnerOrderId(partnerOrderId).tid(subscriptionPaymentDto.getTid())
                .partnerUserId(String.valueOf(subscriptionPaymentDto.getConsumerId())).pgToken(pgToken).build());
        if(response.getStatusCode().value()!=HttpStatus.SC_OK){
            throw new KakaoPayApproveException("카카오 페이 승인 실패");
        }

        LocalDateTime startDate = LocalDateTime.now();
        try {
            subscriptionDtoKafkaTemplate.send(KafkaTopicNameInfo.CREATE_SUBSCRIPTION, SubscriptionDto.builder()
                    .consumerId(subscriptionPaymentDto.getConsumerId())
                    .subscriptionType(SubscriptionTypeEnum.YANGBAN)
                    .paymentAmount(subscriptionPaymentDto.getPaymentAmount())
                    .taxFreeAmount(subscriptionPaymentDto.getPaymentTaxFreeAmount())
                    .startDate(startDate)
                    .endDate(startDate.plusDays(30))
                    .paymentMethod(PaymentMethodEnum.KAKAO)
                    .subscripton(KakaoSubscription.builder()
                            .sid(kakaoPayUtil.getTargetToken(response, "sid"))
                            .cid(subscriptionCid)
                            .tid(subscriptionPaymentDto.getTid())
                            .orderId(partnerOrderId)
                            .build())
            .build());

            memberInfoForNotificationDtoKafkaProcessor.send(KafkaTopicNameInfo.SEND_NOTIFICATION,
                    ServerErrorForNotificationDto.builder()
                            .recipientId(subscriptionPaymentDto.getConsumerId())
                            .recipientType(RecipientTypeEnum.ROLE_CONSUMER)
                            .notificationType(NotificationTypeEnum.SUCCESS_SUBSCRIPTION_PAYMENTS)
                            .createdAt(now())
                    .build());
        }catch(Exception e){
            kakaoPayUtil.callKakaoCancelApi(KakaoPayCancelDto.builder().tid(subscriptionPaymentDto.getTid()).cancelAmount(subscriptionPaymentDto.getPaymentAmount())
                    .cancelTaxFreeAmount(0L).build());
            throw new KafkaException("카프카 예외 발생");
        }
    }

    @Transactional
    public void createPayment(String partnerOrderId, String pgToken, CreditPaymentDto creditPaymentDto) {
        Payment payment = paymentRepository.save(creditPaymentDto.convertPaymentDtoToPayment());
        kakaoPaymentRepository.save(KakaoPayment.builder().payment(payment).tid(creditPaymentDto.getTid()).build());
        if (kakaoPayUtil.callKakaoApproveApi(KakaoPayApproveDto.builder().partnerOrderId(partnerOrderId).tid(creditPaymentDto.getTid())
                        .partnerUserId(String.valueOf(creditPaymentDto.getConsumerId())).pgToken(pgToken).build()) != HttpStatus.SC_OK) {
            throw new KakaoPayApproveException("카카오 페이 승인 실패");
        }

        try {
            creditUpdateDtoKafkaTemplate.send(KafkaTopicNameInfo.UPDATE_CREDIT, CreditUpdateDto.builder()
                    .consumerId(creditPaymentDto.getConsumerId()).credit(creditPaymentDto.getChargeCredit()).tid(creditPaymentDto.getTid())
                    .cancelAmount(creditPaymentDto.getPaymentAmount()).cancelTaxFreeAmount(0L).build());
        }catch(Exception e){
            kakaoPayUtil.callKakaoCancelApi(KakaoPayCancelDto.builder().tid(creditPaymentDto.getTid()).cancelAmount(creditPaymentDto.getPaymentAmount())
                    .cancelTaxFreeAmount(0L).build());
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
        Payment payment = Payment.builder().consumerId(orderInfoDto.getOrderCreationDto().getConsumerId()).paymentType(PaymentTypeEnum.ORDER)
                .paymentMethod(orderInfoDto.getOrderCreationDto().getPaymentMethod()).paymentAmount(realPrice).paymentTaxFreeAmount(0L).build();
        paymentRepository.save(payment);

        // paymentOrder 테이블 생성
        paymentOrderRepository.save(PaymentOrder.builder().payment(payment).ordersId(orderInfoDto.getOrderCreationDto().getOrderId())
                .totalPrice(totalPrice).minusCouponAmount(couponAmount).minusPointAmount(point).couponCode(orderInfoDto.getUserCouponUpdateDto().getCouponCode()).build());

        // kakao_payment 테이블 생성
        if(orderInfoDto.getOrderCreationDto().getPaymentMethod() == PaymentMethodEnum.KAKAO){
            KakaoPayMethod kakaoPayMethod = (KakaoPayMethod) orderInfoDto.getOrderCreationDto().getPaymentInfo();

            kakaoPaymentRepository.save(KakaoPayment.builder().payment(payment).tid(kakaoPayMethod.getTid()).build());

            kakaoPayUtil.callKakaoApproveApi(KakaoPayApproveDto.builder().tid(kakaoPayMethod.getTid()).pgToken(kakaoPayMethod.getPgToken())
                    .partnerOrderId(kakaoPayMethod.getPartnerOrderId()).partnerUserId(kakaoPayMethod.getPartnerUserId()).build());
        }
    }

    public void cancelPayment(OrderCancelDto orderCancelDto){
        PaymentOrder paymentOrder = paymentOrderRepository.findByOrdersId(orderCancelDto.getOrdersId());
        Payment payment = paymentOrder.getPayment();
        long returnValue = paymentOrder.getTotalPrice() - paymentOrder.getMinusPointAmount() - paymentOrder.getMinusCouponAmount();

        if(orderCancelDto.getCancelAmount()!=null && orderCancelDto.getCancelAmount() > 0){ // 취소금액이 존재하면 상품 취소임
            returnValue = orderCancelDto.getCancelAmount();
        }

        cancelPayment(payment,returnValue);
        productUpdateDtoKafkaTemplate.send(KafkaTopicNameInfo.CANCEL_ORDER_STOCK, ProductUpdateListDto.builder()
                .productUpdateDtoList(orderCancelDto.getProductUpdateDtoList())
        .build());
    }

    public Page<CreditChargeHistoryDto> getConsumerCreditHistory(Long consumerId, Pageable pageable){
        return paymentRepository.findCreditChargeHistoryByConsumerIdAndPaymentType(consumerId, PaymentTypeEnum.CREDIT, pageable);
    }

    public PaymentInfoDto getPaymentInfo(String orderId){
        return paymentOrderRepository.findByOrdersIdWithDto(orderId);
    }

    private void cancelPayment(Payment payment, long returnValue){
        if(payment.getPaymentMethod() == PaymentMethodEnum.KAKAO){
            KakaoPayment kakaoPayment = kakaoPaymentRepository.findByPaymentPaymentId(payment.getPaymentId());
            kakaoPayUtil.callKakaoCancelApi(KakaoPayCancelDto.builder().tid(kakaoPayment.getTid()).cancelAmount(returnValue).cancelTaxFreeAmount(0L).build());
        }
    }
}
