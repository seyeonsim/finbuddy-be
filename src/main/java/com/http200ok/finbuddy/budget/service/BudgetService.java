package com.http200ok.finbuddy.budget.service;

import com.http200ok.finbuddy.budget.dto.BudgetResponseDto;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;

import java.util.List;
import java.util.Optional;

public interface BudgetService {
    Long createMonthlyBudget(Long memberId, Long amount);
    Long updateBudget(Long memberId, Long budgetId, Long newAmount);
    Optional<BudgetResponseDto> getCurrentMonthBudgetDto(Long memberId);
    void deleteBudget(Long memberId, Long budgetId);
    void checkAndNotifyBudgetExceededOnTransaction(Long memberId);
    List<CheckingAccountTransactionResponseDto> getLatestTransactionsForCurrentMonth(Long memberId);
    void toggleBudgetNotification(Long memberId, Long budgetId, boolean enabled);
}
