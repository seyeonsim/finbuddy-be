package com.http200ok.finbuddy.mail.controller;

import com.http200ok.finbuddy.mail.dto.MailRequestDto;
import com.http200ok.finbuddy.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    // 인증 이메일 전송
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMail(@RequestBody MailRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            mailService.sendMail(requestDto.getMail());
            response.put("success", true);
            response.put("message", "이메일이 전송되었습니다.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // 인증번호 검증
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody MailRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        boolean isMatch = mailService.verifyCode(requestDto.getMail(), requestDto.getCode());

        response.put("success", isMatch);
        response.put("message", isMatch ? "인증번호가 일치합니다." : "인증번호가 틀렸습니다.");

        return ResponseEntity.ok(response);
    }
}
