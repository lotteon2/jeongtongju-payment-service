package com.jeontongju.payment.controller;

import com.jeontongju.payment.ControllerTestUtil;
import com.jeontongju.payment.dto.MemberCreditChargeDto;
import com.jeontongju.payment.dto.PaymentCreationDto;
import com.jeontongju.payment.dto.ProductDto;
import com.jeontongju.payment.service.PaymentService;
import com.jeontongju.payment.util.KakaoPayUtil;
import com.jeontongju.payment.util.OrderKafkaRouteUtil;
import com.jeontongju.payment.util.RedisUtil;
import io.github.bitbox.bitbox.dto.OrderInfoDto;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KakaoControllerTest extends ControllerTestUtil {
    @MockBean
    private KakaoPayUtil kakaoPayUtil;
    @MockBean
    private RedisUtil redisUtil;
    @MockBean
    private OrderKafkaRouteUtil<OrderInfoDto> orderInfoDtoKafkaRouteUtil;
    @MockBean
    private PaymentService paymentService;


    @Test
    void ROLE_MANAGER는_주문이_불가능하다() throws Exception{
        checkControllerFailConditions("/api/order", 1L, MemberRoleEnum.ROLE_SELLER,
                createPaymentCreationDto(PaymentTypeEnum.ORDER, PaymentMethodEnum.KAKAO, 1L, "123",10L,
                        "test","test","test","test","","12345",10L,10L,
                        "test","10",10L),
                "주문은 소비자만 할 수 있습니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void ROLE_ADMIN은_주문이_불가능하다() throws Exception{
        checkControllerFailConditions("/api/order", 1L, MemberRoleEnum.ROLE_ADMIN,
                createPaymentCreationDto(PaymentTypeEnum.ORDER, PaymentMethodEnum.KAKAO, 1L, "123",10L,
                        "test","test","test","test","","12345",10L,10L,
                        "test","10",10L),
                "주문은 소비자만 할 수 있습니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void ROLE_USER는_주문이_가능하다() throws Exception{
        checkControllerSuccessConditions("/api/order", 1L, MemberRoleEnum.ROLE_CONSUMER,
                createPaymentCreationDto(PaymentTypeEnum.ORDER, PaymentMethodEnum.KAKAO, null, null,null,
                        "test","test","test","test","","12345",10L,10L,
                        "test","10",10L),new LinkedMultiValueMap<>(),
                status().isOk()
        );
    }

    @Test
    void 쿠폰번호가_있는경우에는_쿠폰금액은_필수이다() throws Exception{
        checkControllerFailConditions("/api/order", 1L, MemberRoleEnum.ROLE_CONSUMER,
                createPaymentCreationDto(PaymentTypeEnum.ORDER, PaymentMethodEnum.KAKAO, null, "123",null,
                        "test","test","test","test","","12345",10L,10L,
                        "test","10",10L),
                "쿠폰 관련 정보가 이상합니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void 쿠폰금액이_있는경우에는_쿠폰번호는_필수이다() throws Exception{
        checkControllerFailConditions("/api/order", 1L, MemberRoleEnum.ROLE_CONSUMER,
                createPaymentCreationDto(PaymentTypeEnum.ORDER, PaymentMethodEnum.KAKAO, 100L, null,100L,
                        "test","test","test","test","","12345",10L,10L,
                        "test","10",10L),
                "쿠폰 관련 정보가 이상합니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void ROLE_MANAGER는_크레딧충전이_불가능하다() throws Exception {
        checkControllerFailConditions("/api/credit", 1L, MemberRoleEnum.ROLE_SELLER,
                createMemberCreditChargeDto(10L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.KAKAO),
                "크레딧 충전은 소비자만 할 수 있습니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void ROLE_ADMIN은_크레딧충전이_불가능하다() throws Exception {
        checkControllerFailConditions("/api/credit", 1L, MemberRoleEnum.ROLE_ADMIN,
                createMemberCreditChargeDto(10L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.NAVER),
                "크레딧 충전은 소비자만 할 수 있습니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void ROLE_USER는_크레딧충전이_가능하다() throws Exception {
        checkControllerSuccessConditions("/api/credit", 1L, MemberRoleEnum.ROLE_CONSUMER,
                createMemberCreditChargeDto(10L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.NAVER),new LinkedMultiValueMap<>(),
                status().isOk()
        );
    }

    @Test
    void 크레딧_충전액이_0이거나_음수면_크레딧_충전이_불가능하다() throws Exception {
        checkControllerFailConditions("/api/credit",1L, MemberRoleEnum.ROLE_CONSUMER,
                createMemberCreditChargeDto(0L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.KAKAO),
                "최소 크레딧은 1이상입니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
        checkControllerFailConditions("/api/credit",1L, MemberRoleEnum.ROLE_CONSUMER,
                createMemberCreditChargeDto(-1L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.KAKAO),
                "최소 크레딧은 1이상입니다.",new LinkedMultiValueMap<>(),status().isBadRequest()
        );
    }

    @Test
    void 크레딧_충전액이_양수면_크레딧_충전이_가능하다() throws Exception {
        checkControllerSuccessConditions("/api/credit",1L, MemberRoleEnum.ROLE_CONSUMER,
                createMemberCreditChargeDto(1000L, PaymentTypeEnum.CREDIT, "test",PaymentMethodEnum.KAKAO),new LinkedMultiValueMap<>(),
                status().isOk()
        );
    }

    private PaymentCreationDto createPaymentCreationDto(PaymentTypeEnum paymentType, PaymentMethodEnum paymentMethod,
                                                        Long pointUsageAmount, String couponCode, Long couponAmount, String productImg,
                                                        String recipientName, String recipientPhoneNumber, String basicAddress,
                                                        String addressDetail, String zoneCode, Long totalAmount,Long realAmount,
                                                        String titleName, String productId, Long productCount
                                                        )
    {
        List<ProductDto> list = Arrays.asList(ProductDto.builder().productId(productId).productCount(productCount).build());

        return PaymentCreationDto.builder()
                .paymentType(paymentType)
                .paymentMethod(paymentMethod)
                .pointUsageAmount(pointUsageAmount)
                .couponCode(couponCode)
                .couponAmount(couponAmount)
                .productImg(productImg)
                .recipientName(recipientName)
                .recipientPhoneNumber(recipientPhoneNumber)
                .basicAddress(basicAddress)
                .addressDetail(addressDetail)
                .zoneCode(zoneCode)
                .totalAmount(totalAmount)
                .realAmount(realAmount)
                .titleName(titleName)
                .products(list)
                .build();
    }

    private MemberCreditChargeDto createMemberCreditChargeDto(Long chargeCredit, PaymentTypeEnum paymentType,
                                                              String itemName, PaymentMethodEnum paymentMethodEnum){
        return MemberCreditChargeDto.builder()
                .chargeCredit(chargeCredit)
                .paymentType(paymentType)
                .itemName(itemName)
                .paymentMethod(paymentMethodEnum)
                .build();
    }
}
