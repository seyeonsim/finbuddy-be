package com.http200ok.finbuddy.budget.repository;

import com.http200ok.finbuddy.budget.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByMemberIdAndStartDate(Long memberId, LocalDate startDate);
}
