package com.http200ok.finbuddy.auth.service;

import com.http200ok.finbuddy.auth.domain.RefreshToken;
import com.http200ok.finbuddy.auth.dto.SignInRequest;
import com.http200ok.finbuddy.auth.repository.RefreshTokenRepository;
import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import com.http200ok.finbuddy.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public ResponseEntity<?> signIn(SignInRequest request, HttpServletResponse response) {
        try {
            // 1. 사용자 인증 진행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            // 401 Unauthorized 상태 코드 반환
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 2. 사용자 정보 가져오기
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        // 4. 리프레시 토큰을 DB에 저장
        refreshTokenRepository.findByMember(member)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(member, refreshToken))
                );

        // 5. jwt 토큰을 HttpOnly 쿠키로 설정
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1시간
//        accessTokenCookie.setSecure(false);
//        accessTokenCookie.setAttribute("SameSite", "None");


        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
//        refreshTokenCookie.setSecure(false);
//        refreshTokenCookie.setAttribute("SameSite", "None");

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok().build(); // 응답 바디에는 아무것도 반환하지 않음

    }



    @Transactional
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. 요청에서 리프레시 토큰 추출
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰");
        }

        // 2. 리프레시 토큰에서 사용자 ID 추출
        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 3. 데이터베이스에서 저장된 리프레시 토큰 검증
        RefreshToken storedToken = refreshTokenRepository.findByMember(member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."));

        if (!storedToken.getToken().equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 불일치");
        }

        // 4. 새 액세스 토큰 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(member.getId());

        // 5. 액세스 토큰을 HttpOnly 쿠키로 저장
        Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1시간

        response.addCookie(accessTokenCookie);

        return ResponseEntity.ok().build(); // ✅ 응답 바디에는 아무것도 반환하지 않음
    }

    // 리프레시 토큰 추출
    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }


    @Transactional
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 요청에서 리프레시 토큰 추출
        String refreshToken = extractRefreshToken(request);

        if (refreshToken != null) {
            try {
                // 2. 리프레시 토큰에서 사용자 ID 추출, 데이터베이스에서 리프레시 토큰 제거
                Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
                memberRepository.findById(memberId).flatMap(refreshTokenRepository::findByMember)
                        .ifPresent(refreshTokenRepository::delete);

            } catch (Exception e) {
                System.out.println("❌ [ERROR] 로그아웃 처리 중 오류 발생: " + e.getMessage());

            }
        }

        // 4. 액세스 & 리프레시 토큰 쿠키 삭제 (브라우저에서 자동 삭제되도록 설정)
        Cookie accessTokenCookie = new Cookie("accessToken", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // 즉시 만료

        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 즉시 만료

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok().build();
    }

}