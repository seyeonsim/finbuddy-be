package com.http200ok.finbuddy.common.validator;

import com.http200ok.finbuddy.budget.domain.Budget;
import com.http200ok.finbuddy.budget.repository.BudgetRepository;
import com.http200ok.finbuddy.common.exception.DuplicateBudgetException;
import com.http200ok.finbuddy.common.exception.UnauthorizedAccessException;
import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetValidatorImpl implements BudgetValidator {

    private final BudgetRepository budgetRepository;
    private final MemberRepository memberRepository;

    @Override
    public Budget validateAndGetBudget(Long budgetId, Long memberId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));

        if (!budget.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException("Member " + memberId + " is not authorized to access budget " + budgetId);
        }

        return budget;
    }

    @Override
    public Member validateMemberAndCheckDuplicateBudget(Long memberId) {
        // 1. Validate member exists
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

        // 2. Check for duplicate budget in current month
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);

        Optional<Budget> existingBudget = budgetRepository.findByMemberIdAndStartDate(memberId, startOfMonth);
        if (existingBudget.isPresent()) {
            throw new DuplicateBudgetException("Budget already exists for the current month. Budget id: " + existingBudget.get().getId());
        }

        return member;
    }
}