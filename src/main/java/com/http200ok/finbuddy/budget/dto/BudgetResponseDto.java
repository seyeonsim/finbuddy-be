package com.http200ok.finbuddy.budget.dto;

import com.http200ok.finbuddy.budget.domain.Budget;
import com.http200ok.finbuddy.budget.domain.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponseDto {
    private Long id;
    private Long memberId;
    private Long amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodType periodType;
    private boolean notificationEnabled;
    private Long spentAmount;

    public static BudgetResponseDto fromEntity(Budget budget, Long spentAmount) {
        return new BudgetResponseDto(
                budget.getId(),
                budget.getMember().getId(),
                budget.getAmount(),
                budget.getStartDate(),
                budget.getEndDate(),
                budget.getPeriodType(),
                budget.isNotificationEnabled(),
                spentAmount
        );
    }
}
