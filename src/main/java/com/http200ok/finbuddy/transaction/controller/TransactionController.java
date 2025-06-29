package com.http200ok.finbuddy.transaction.controller;

import com.http200ok.finbuddy.category.dto.CategoryExpenseDto;
import com.http200ok.finbuddy.common.dto.PagedResponseDto;
import com.http200ok.finbuddy.security.CustomUserDetails;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;
import com.http200ok.finbuddy.transaction.dto.MonthlyTransactionSummaryDto;
import com.http200ok.finbuddy.transaction.dto.TransactionResponseDto;
import com.http200ok.finbuddy.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/checking/recent")
    public ResponseEntity<List<CheckingAccountTransactionResponseDto>> getLatestTransactionsForUserCheckingAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<CheckingAccountTransactionResponseDto> transactions = transactionService.getLatestTransactionsForUserCheckingAccounts(memberId);
        return ResponseEntity.ok(transactions);
    }

    // 카테고리별 지출 금액, 비율 조회
    @GetMapping("/category-expenses")
    public ResponseEntity<List<CategoryExpenseDto>> getCategoryExpenses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        Long memberId = userDetails.getMemberId();
        List<CategoryExpenseDto> expenses = transactionService.categoryExpensesForMonth(memberId, year, month);

        return ResponseEntity.ok(expenses);
    }

    // 카테고리별 지출 금액, 비율 조회(월, 계좌)
    @GetMapping("/account-category-expense")
    public ResponseEntity<List<CategoryExpenseDto>> getAccountCategoryExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("accountId") Long accountId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        Long memberId = userDetails.getMemberId();
        List<CategoryExpenseDto> expenses = transactionService.categoryExpensesForAccountAndMonth(memberId, accountId, year, month);

        return ResponseEntity.ok(expenses);
    }

    @GetMapping("account/{accountId}")
    public ResponseEntity<PagedResponseDto<TransactionResponseDto>> getTransactionsByAccountId(
            @PathVariable("accountId") Long accountId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "transactionType", required = false) Integer transactionType,
            @PageableDefault(page = 0, size = 10, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Long memberId = userDetails.getMemberId();
        Page<TransactionResponseDto> pagedTransactions = transactionService.getTransactionsByAccountId(accountId, memberId, startDate, endDate, transactionType, pageable);

        return ResponseEntity.ok(new PagedResponseDto<>(pagedTransactions));
    }

    @GetMapping("/account/monthly-summary")
    public ResponseEntity<MonthlyTransactionSummaryDto> getMonthlySummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("accountId") Long accountId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        Long memberId = userDetails.getMemberId();
        MonthlyTransactionSummaryDto summary = transactionService.getMonthlyTransactionSummary(memberId, accountId, year, month);

        return ResponseEntity.ok(summary);
    }
}
