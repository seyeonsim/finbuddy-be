package com.http200ok.finbuddy.budget.service;

import com.http200ok.finbuddy.budget.domain.Budget;
import com.http200ok.finbuddy.budget.domain.PeriodType;
import com.http200ok.finbuddy.budget.dto.BudgetResponseDto;
import com.http200ok.finbuddy.budget.repository.BudgetRepository;
import com.http200ok.finbuddy.common.validator.BudgetValidator;
import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.notification.domain.NotificationType;
import com.http200ok.finbuddy.notification.service.NotificationService;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetValidator budgetValidator;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Long createMonthlyBudget(Long memberId, Long amount) {
        Member member = budgetValidator.validateMemberAndCheckDuplicateBudget(memberId);

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        Budget budget = Budget.createBudget(member, amount, PeriodType.MONTHLY, startDate, endDate);
        budgetRepository.save(budget);

        return budget.getId();
    }

    @Override
    @Transactional
    public Long updateBudget(Long memberId, Long budgetId, Long newAmount) {
        Budget budget = budgetValidator.validateAndGetBudget(budgetId, memberId);
        budget.setAmount(newAmount);
        budget.setNotificationEnabled(true); // 예산 수정 시 알림 활성화
        return budget.getId();
    }

    @Override
    @Transactional
    public void deleteBudget(Long memberId, Long budgetId) {
        Budget budget = budgetValidator.validateAndGetBudget(budgetId, memberId);
        budgetRepository.delete(budget);
    }

    // 현재 월의 예산 조회 (화면 반환용 - DTO 변환)
    @Override
    @Transactional(readOnly = true)
    public Optional<BudgetResponseDto> getCurrentMonthBudgetDto(Long memberId) {
        Long totalSpending = transactionRepository.getTotalSpendingForCurrentMonth(memberId);
        return getCurrentMonthBudget(memberId)
                .map(budget -> BudgetResponseDto.fromEntity(budget, totalSpending != null ? totalSpending : 0L));
    }

    // 현재 월의 예산 조회 (내부 로직용 - Entity 반환)
    @Transactional(readOnly = true)
    private Optional<Budget> getCurrentMonthBudget(Long memberId) {
        LocalDate now = LocalDate.now();
        return budgetRepository.findByMemberIdAndStartDate(memberId, now.withDayOfMonth(1));
    }

    // 이체 발생 즉시 예산 초과 여부 확인 및 알림 전송
    @Override
    @Transactional
    public void checkAndNotifyBudgetExceededOnTransaction(Long memberId) {
        getCurrentMonthBudget(memberId)
                .ifPresent(budget -> {
                    // 알림이 비활성화된 경우 예산 초과 확인을 건너뜀
                    if (!budget.isNotificationEnabled()) {
                        return;
                    }

                    Long totalSpending = transactionRepository.getTotalSpendingForCurrentMonth(memberId);
                    Long budgetLimit = budget.getAmount();

                    if (totalSpending > budgetLimit) {
                        Long exceededAmount = totalSpending - budgetLimit;
                        int currentMonth = LocalDate.now().getMonthValue();

                        String message = String.format(
                                "%d월 예산을 %,d원 초과하였습니다. (예산: %,d원, 현재 지출: %,d원)",
                                currentMonth, exceededAmount, budgetLimit, totalSpending
                        );

                        notificationService.sendNotification(
                                budget.getMember(), NotificationType.BUDGET, message
                        );
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckingAccountTransactionResponseDto> getLatestTransactionsForCurrentMonth(Long memberId) {
        // 현재 날짜 가져오기
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // 해당 월의 거래 내역 조회
        return transactionRepository.findLatestTransactionsForUserCheckingAccountsInMonth(memberId, currentYear, currentMonth)
                .stream()
                .map(CheckingAccountTransactionResponseDto::new)
                .collect(Collectors.toList());
    }

    // 알림 설정 변경 메소드
    @Override
    @Transactional
    public void toggleBudgetNotification(Long memberId, Long budgetId, boolean enabled) {
        Budget budget = budgetValidator.validateAndGetBudget(budgetId, memberId);
        budget.toggleNotification(enabled);
    }
}
