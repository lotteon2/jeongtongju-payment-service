package com.jeontongju.payment.dto.temp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberPointUpdateDto {
    private Long memberId;
    private Long usePoint;
    private boolean doConsume;

    public static MemberPointUpdateDto OrderCreationDtoToMemberPointUpdateDto(Long memberId, Long usePoint, boolean doConsume){
        return MemberPointUpdateDto.builder().memberId(memberId).usePoint(usePoint).doConsume(doConsume).build();
    }
}
