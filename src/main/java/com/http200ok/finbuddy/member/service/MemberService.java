package com.http200ok.finbuddy.member.service;

import com.http200ok.finbuddy.member.dto.SignUpRequest;
import com.http200ok.finbuddy.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;

public interface MemberService {
    void signUp(SignUpRequest request);
    ResponseEntity<?> getCurrentUser(CustomUserDetails userDetails);
}
