package com.http200ok.finbuddy.budget.domain;

import com.http200ok.finbuddy.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    private Long amount;

    // 알림 활성화 여부 필드, 기본값 true
    @Column(nullable = false)
    private boolean notificationEnabled = true;

    // 생성 메서드
    public static Budget createBudget(Member member, Long amount, PeriodType periodType, LocalDate startDate, LocalDate endDate) {
        Budget budget = new Budget();
        budget.member = member;
        budget.amount = amount;
        budget.periodType = periodType;
        budget.startDate = startDate;
        budget.endDate = endDate;
        budget.notificationEnabled = true;
        return budget;
    }

    public void toggleNotification(boolean enabled) {
        this.notificationEnabled = enabled;
    }
}
