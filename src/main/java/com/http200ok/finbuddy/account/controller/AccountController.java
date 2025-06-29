package com.http200ok.finbuddy.account.controller;

import com.http200ok.finbuddy.account.dto.AccountResponseDto;
import com.http200ok.finbuddy.account.dto.AccountSummaryResponseDto;
import com.http200ok.finbuddy.account.dto.CheckingAccountsSummaryResponseDto;
import com.http200ok.finbuddy.account.dto.PasswordVerificationRequest;
import com.http200ok.finbuddy.account.service.AccountService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDto> getAccountDetails(@PathVariable("accountId") Long accountId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        AccountResponseDto accountDetails = accountService.getAccountDetails(memberId, accountId);
        return ResponseEntity.ok(accountDetails);
    }

    @GetMapping("/checking")
    public ResponseEntity<CheckingAccountsSummaryResponseDto> getCheckingAccountsTop3(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        CheckingAccountsSummaryResponseDto checkingAccountsSummary = accountService.getCheckingAccountsSummary(memberId);
        return ResponseEntity.ok(checkingAccountsSummary);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AccountSummaryResponseDto>> getAllAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<AccountSummaryResponseDto> accounts = accountService.getAccountsByMemberId(memberId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * 계좌 비밀번호 검증 API
     */
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody PasswordVerificationRequest request) {
        boolean isValid = accountService.verifyPassword(request.getAccountId(), request.getPassword());
        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
