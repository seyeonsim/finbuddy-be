package com.http200ok.finbuddy.transaction.service;

import com.http200ok.finbuddy.category.dto.CategoryExpenseDto;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;
import com.http200ok.finbuddy.transaction.dto.MonthlyTransactionSummaryDto;
import com.http200ok.finbuddy.transaction.dto.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    List<CheckingAccountTransactionResponseDto> getLatestTransactionsForUserCheckingAccounts(Long memberId);
    List<CategoryExpenseDto> categoryExpensesForMonth(Long memberId, int year, int month);
    List<CategoryExpenseDto> categoryExpensesForAccountAndMonth(Long memberId, Long accountId, int year, int month);
    Page<TransactionResponseDto> getTransactionsByAccountId(Long accountId, Long memberId, LocalDate startDate, LocalDate endDate, Integer transactionType, Pageable pageable);
    MonthlyTransactionSummaryDto getMonthlyTransactionSummary(Long memberId, Long accountId, int year, int month);
}
