package com.http200ok.finbuddy.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    // `application.properties`에서 값을 불러와서 설정
    public JwtTokenProvider(
            @Value("${jwt.access.secret}") String accessSecret,
            @Value("${jwt.refresh.secret}") String refreshSecret,
            @Value("${jwt.access.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh.expiration}") long refreshTokenExpiration) {

        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(refreshSecret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 액세스 토큰 생성 (1시간 유지)
    public String generateAccessToken(Long memberId) {
        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(accessKey)
                .compact();
    }

    // 리프레시 토큰 생성 (7일 유지)
    public String generateRefreshToken(Long memberId) {
        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(refreshKey)
                .compact();
    }


    // 요청에서 액세스 토큰 추출
    public Optional<String> resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7));
        }
        return Optional.empty();
    }

    // 액세스 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(accessKey).build().parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 액세스 토큰에서 memberId 추출
    public Long getMemberIdFromAccessToken(String token) {
        return Long.parseLong(Jwts.parser().verifyWith(accessKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    // 리프레시 토큰 검증
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser().verifyWith(refreshKey).build().parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 리프레시 토큰에서 memberId 추출
    public Long getMemberIdFromRefreshToken(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }
}
