package com.http200ok.finbuddy.auth.service;

import com.http200ok.finbuddy.auth.dto.SignInRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
    ResponseEntity<?> signIn(SignInRequest request, HttpServletResponse response);
    ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response);
    ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response);
}
