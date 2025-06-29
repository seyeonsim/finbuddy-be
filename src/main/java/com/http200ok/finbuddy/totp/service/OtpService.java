package com.http200ok.finbuddy.totp.service;

import com.http200ok.finbuddy.member.domain.Member;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public interface OtpService {
    boolean isOtpRegistered(Member member);
    GoogleAuthenticatorKey generateKey();
    String generateQRCode(Member member, GoogleAuthenticatorKey key);
    boolean verifyCode(String secret, int code);
    void saveOtpSecret(Member member, String secret);
}
