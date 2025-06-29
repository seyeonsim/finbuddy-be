package com.http200ok.finbuddy.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordVerificationRequest {
    private Long accountId;
    private String password;
}