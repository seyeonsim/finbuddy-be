package com.http200ok.finbuddy.common.validator;

import com.http200ok.finbuddy.budget.domain.Budget;
import com.http200ok.finbuddy.member.domain.Member;

public interface BudgetValidator {
    Budget validateAndGetBudget(Long budgetId, Long memberId);
    Member validateMemberAndCheckDuplicateBudget(Long memberId);
}
