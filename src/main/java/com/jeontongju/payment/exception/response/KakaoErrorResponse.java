package com.jeontongju.payment.exception.response;

import io.github.bitbox.bitbox.enums.FailureTypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KakaoErrorResponse {
    private final String tid;
    private final Boolean tms_result;
    private final String next_redirect_app_url;
    private final String next_redirect_mobile_url;
    private final String next_redirect_pc_url;
    private final String android_app_scheme;
    private final String ios_app_scheme;
    private final String created_at;
    private final FailureTypeEnum message;

}