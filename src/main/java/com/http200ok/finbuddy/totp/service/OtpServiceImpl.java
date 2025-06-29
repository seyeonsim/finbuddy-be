package com.http200ok.finbuddy.totp.service;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.totp.domain.Otp;
import com.http200ok.finbuddy.totp.repository.OtpRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator(); // Google Authenticator 인스턴스


    public boolean isOtpRegistered(Member member) {
        return otpRepository.existsByMember(member);
    }


    public GoogleAuthenticatorKey generateKey() {
        return gAuth.createCredentials();
    }


    public String generateQRCode(Member member, GoogleAuthenticatorKey key) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("Finbuddy", member.getEmail(), key);
    }


    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }


    @Transactional
    public void saveOtpSecret(Member member, String secret) {
        // 기존 OTP 정보 삭제 (있을 경우)
        otpRepository.deleteByMember(member);

        // 새로운 OTP 정보 저장
        Otp otp = Otp.builder()
                .member(member)
                .secret(secret)
                .build();

        otpRepository.save(otp);
    }
}
