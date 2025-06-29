package com.http200ok.finbuddy.member.controller;

import com.http200ok.finbuddy.member.dto.SignUpRequest;
import com.http200ok.finbuddy.member.service.MemberService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        memberService.signUp(request);
        return ResponseEntity.ok().body("{\"success\": true, \"message\": \"회원가입 성공\"}");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberService.getCurrentUser(userDetails);
    }

}
