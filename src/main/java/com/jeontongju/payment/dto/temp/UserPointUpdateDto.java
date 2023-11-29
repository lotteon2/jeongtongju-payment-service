package com.jeontongju.payment.dto.temp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 domain : consumer
 detail : 포인트 사용을 위한 DTO임 포인트를 확인 후 포인트를 차감한다
 method : kafka
 comment :
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointUpdateDto {
    private Long consumerId;
    private Long point;
}