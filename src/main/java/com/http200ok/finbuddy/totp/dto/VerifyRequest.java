package com.http200ok.finbuddy.totp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {
    private int otpCode; // OTP는 숫자 6자리이므로 int 타입 사용
}