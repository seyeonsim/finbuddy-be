package com.http200ok.finbuddy.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    @Value("${smtp.email}")
    private String senderEmail;

    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final Map<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private record VerificationData(String code, Instant expirationTime) {}

    // 인증번호 생성
    private String generateCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    // 이메일 생성
    private MimeMessage createMail(String mail, String code) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(mail);
            helper.setSubject("이메일 인증");
            String body = "<h2>FinBuddy</h2>" +
                    "<h3>요청하신 인증 번호입니다.</h3>" +
                    "<h1>" + code + "</h1>" +
                    "<h3>5분 이내에 입력해주세요.</h3>" +
                    "<h3>감사합니다.</h3>";
            helper.setText(body, true);
        } catch (MessagingException e) {
            throw new RuntimeException("메일 전송 실패", e);
        }

        return message;
    }

    @Override
    public void sendMail(String mail) {
        String code = generateCode();
        Instant expirationTime = Instant.now().plusSeconds(CODE_EXPIRATION_MINUTES * 60);

        verificationCodes.put(mail, new VerificationData(code, expirationTime));
        scheduleCodeExpiration(mail, expirationTime);

        MimeMessage message = createMail(mail, code);
        javaMailSender.send(message);
    }

    private void scheduleCodeExpiration(String mail, Instant expirationTime) {
        long delay = expirationTime.getEpochSecond() - Instant.now().getEpochSecond();
        scheduler.schedule(() -> verificationCodes.remove(mail), delay, TimeUnit.SECONDS);
    }

    @Override
    public boolean verifyCode(String mail, String userCode) {
        VerificationData data = verificationCodes.get(mail);

        if (data == null || Instant.now().isAfter(data.expirationTime())) {
            verificationCodes.remove(mail); // 만료된 코드 제거
            return false;
        }

        boolean isValid = data.code().equals(userCode);
        if (isValid) {
            verificationCodes.remove(mail); // 성공 시 코드 삭제
        }
        return isValid;
    }
}
