package com.http200ok.finbuddy.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import javax.crypto.SecretKey;

public class SecretKeyGenerator {

    // SecretKey 생성 메서드
    public static String generateSecretKey() {
        SecretKey key = Jwts.SIG.HS256.key().build();
        return Encoders.BASE64URL.encode(key.getEncoded());
    }

    public static void printGeneratedKeys() {
        String accessKey = generateSecretKey();
        String refreshKey = generateSecretKey();

        // 암호화 추가 가능

        System.out.println("✅ JWT_ACCESS_SECRETKEY: " + accessKey);
        System.out.println("✅ JWT_REFRESH_SECRETKEY: " + refreshKey);
    }
}
