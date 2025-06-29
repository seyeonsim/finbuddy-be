package com.http200ok.finbuddy.auth.controller;

import com.http200ok.finbuddy.auth.dto.SignInRequest;
import com.http200ok.finbuddy.auth.dto.SignInResponse;
import com.http200ok.finbuddy.auth.dto.TokenResponse;
import com.http200ok.finbuddy.auth.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest request, HttpServletResponse response) {
        return authenticationService.signIn(request, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.refreshAccessToken(request, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.logout(request, response);
    }

}