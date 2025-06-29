package com.http200ok.finbuddy.budget.controller;

import com.http200ok.finbuddy.budget.dto.BudgetResponseDto;
import com.http200ok.finbuddy.budget.service.BudgetService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // 예산 생성
    @PostMapping
    public ResponseEntity<Long> createMonthlyBudget(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("amount") Long amount) {
        Long memberId = userDetails.getMemberId();
        Long budgetId = budgetService.createMonthlyBudget(memberId, amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetId);
    }

    // 예산 수정
    @PatchMapping("/{budgetId}")
    public ResponseEntity<Long> updateBudget(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("budgetId") Long budgetId, @RequestParam("newAmount") Long newAmount) {
        Long memberId = userDetails.getMemberId();
        Long updatedBudgetId = budgetService.updateBudget(memberId, budgetId, newAmount);
        return ResponseEntity.ok(updatedBudgetId);
    }

    // 예산 알림 설정 변경 엔드포인트 추가
    @PatchMapping("/{budgetId}/notification")
    public ResponseEntity<Void> toggleBudgetNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("budgetId") Long budgetId,
            @RequestParam("enabled") boolean enabled) {
        Long memberId = userDetails.getMemberId();
        budgetService.toggleBudgetNotification(memberId, budgetId, enabled);
        return ResponseEntity.ok().build();
    }

    // 현재 월 예산 조회
    @GetMapping("/current")
    public ResponseEntity<BudgetResponseDto> getCurrentMonthBudget(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Optional<BudgetResponseDto> budgetDto = budgetService.getCurrentMonthBudgetDto(memberId);
        return budgetDto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // 예산 삭제
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("budgetId") Long budgetId) {
        Long memberId = userDetails.getMemberId();
        budgetService.deleteBudget(memberId, budgetId);
        return ResponseEntity.noContent().build();
    }

    // 예산 초과 테스트 API (GET 요청)
//    @GetMapping("/test-exceeded/{memberId}")
//    public ResponseEntity<String> testExceededBudget(@PathVariable("memberId") Long memberId) {
//        budgetService.checkAndNotifyBudgetExceededOnTransaction(memberId);
//        return ResponseEntity.ok("예산 초과 체크를 수행했습니다.");
//    }

    @GetMapping("/checking/recent")
    public ResponseEntity<List<CheckingAccountTransactionResponseDto>> getLatestTransactionsForUserCheckingAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<CheckingAccountTransactionResponseDto> transactions = budgetService.getLatestTransactionsForCurrentMonth(memberId);
        return ResponseEntity.ok(transactions);
    }

}
