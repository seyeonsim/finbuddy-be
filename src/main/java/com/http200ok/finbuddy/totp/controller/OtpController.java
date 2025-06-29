package com.http200ok.finbuddy.totp.controller;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import com.http200ok.finbuddy.security.CustomUserDetails;
import com.http200ok.finbuddy.totp.domain.Otp;
import com.http200ok.finbuddy.totp.dto.VerifyRequest;
import com.http200ok.finbuddy.totp.repository.OtpRepository;
import com.http200ok.finbuddy.totp.service.OtpService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;
    private final MemberRepository memberRepository;
    // Secret을 캐시에 저장하는 ConcurrentHashMap 사용 (또는 Redis 활용 가능)
    private final ConcurrentHashMap<String, String> tempSecretCache = new ConcurrentHashMap<>();
    private final OtpRepository otpRepository;

    @GetMapping("/status")
    public ResponseEntity<?> checkOtpStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        boolean isRegistered = otpService.isOtpRegistered(member);

        return ResponseEntity.ok(isRegistered);

    }


    @GetMapping("/register")
    public ResponseEntity<?> register(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // `GoogleAuthenticatorKey` 객체 생성 (Secret 포함)
        GoogleAuthenticatorKey key = otpService.generateKey();

        // QR 코드 생성 (같은 Key 객체 사용)
        String qrCode = otpService.generateQRCode(member, key);

        // Secret을 캐시에 저장 (DB 저장 X)
        tempSecretCache.put(member.getEmail(), key.getKey());

        return ResponseEntity.ok(qrCode);
    }


    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyAndSaveOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VerifyRequest request) {

        Long memberId = userDetails.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. Secret을 캐시에서 가져옴
        String secret = tempSecretCache.get(member.getEmail());
        if (secret == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("잘못된 요청입니다. 먼저 QR 코드를 등록하세요.");
        }

        // 2. OTP 검증
        boolean isValid = otpService.verifyCode(secret, request.getOtpCode());
        if (isValid) {
            // 3. 인증 성공 시 Secret을 DB에 저장 (OtpService 활용)
            otpService.saveOtpSecret(member, secret);

            // 4. Secret을 캐시에서 제거
            tempSecretCache.remove(member.getEmail());

            return ResponseEntity.ok("OTP 등록 성공! 이제 로그인 시 사용할 수 있습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("OTP 인증 실패. 다시 입력하세요.");
        }
    }


    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VerifyRequest request) {

        Long memberId = userDetails.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. DB에서 사용자의 Secret 가져오기
        Otp otp = otpRepository.findByMember(member)
                .orElseThrow(() -> new RuntimeException("OTP 정보가 등록되지 않은 사용자입니다."));

        // 2. OTP 코드 검증
        boolean isValid = otpService.verifyCode(otp.getSecret(), request.getOtpCode());

        if (isValid) {
            return ResponseEntity.ok("OTP 인증 성공!");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("OTP 인증 실패. 다시 입력하세요.");
        }
    }






}
