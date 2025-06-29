package com.http200ok.finbuddy.transaction.service;

import com.http200ok.finbuddy.category.dto.CategoryExpenseDto;
import com.http200ok.finbuddy.common.validator.AccountValidator;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import com.http200ok.finbuddy.transaction.dto.CheckingAccountTransactionResponseDto;
import com.http200ok.finbuddy.transaction.dto.TransactionResponseDto;
import com.http200ok.finbuddy.transaction.dto.MonthlyTransactionSummaryDto;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;

    @Override
    public List<CheckingAccountTransactionResponseDto> getLatestTransactionsForUserCheckingAccounts(Long memberId) {
        Pageable pageable = PageRequest.of(0, 5);
        List<Transaction> transactions = transactionRepository
                .findLatestTransactionsForUserCheckingAccounts(memberId, pageable)
                .getContent(); // Page 객체에서 List로 변환

        return transactions.stream()
                .map(CheckingAccountTransactionResponseDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryExpenseDto> categoryExpensesForMonth(Long memberId, int year, int month) {
        // 전체 소비 금액 조회
        Long totalAmount = transactionRepository.sumTotalAmountForMonth(memberId, year, month);
        if (totalAmount == null || totalAmount == 0) {
            return Collections.emptyList();
        }

        // 카테고리별 합계 조회
        List<CategoryExpenseDto> categorySums = transactionRepository.sumAmountByCategoryForMonth(memberId, year, month);

        // 각 카테고리별 금액을 전체 금액으로 나누어 비율 계산
        return categorySums.stream()
                .map(dto -> new CategoryExpenseDto(
                        dto.getCategoryName(),
                        dto.getTotalAmount(),
                        totalAmount > 0 ? (dto.getTotalAmount().doubleValue() / totalAmount.doubleValue()) * 100.0 : 0.0
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryExpenseDto> categoryExpensesForAccountAndMonth(Long memberId, Long accountId, int year, int month) {
        Long totalAmount = transactionRepository.getTotalSpendingForMonth(memberId, accountId, year, month);
        if (totalAmount == null || totalAmount == 0) {
            return Collections.emptyList();
        }

        List<CategoryExpenseDto> categorySums = transactionRepository.getTotalSpendingByCategoryForMonth(memberId, accountId, year, month);

        return categorySums.stream()
                .map(dto -> new CategoryExpenseDto(
                        dto.getCategoryName(),
                        dto.getTotalAmount(),
                        totalAmount > 0 ? (dto.getTotalAmount().doubleValue() / totalAmount.doubleValue()) * 100.0 : 0.0
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Page<TransactionResponseDto> getTransactionsByAccountId(Long accountId, Long memberId, LocalDate startDate, LocalDate endDate, Integer transactionType, Pageable pageable) {

        // AccountValidator를 사용하여 계좌 소유권 검증
        accountValidator.validateAndGetAccount(accountId, memberId);

        // LocalDate -> LocalDateTime 변환
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        return transactionRepository.findTransactions(accountId, startDateTime, endDateTime, transactionType, pageable)
                .map(TransactionResponseDto::fromEntity);
    }

    public MonthlyTransactionSummaryDto getMonthlyTransactionSummary(Long memberId, Long accountId, int year, int month) {
        Long totalSpending = transactionRepository.getTotalSpendingForMonth(memberId, accountId, year, month);
        Long totalIncome = transactionRepository.getTotalIncomeForMonth(memberId, accountId, year, month);

        return new MonthlyTransactionSummaryDto(totalIncome, totalSpending);
    }
}
