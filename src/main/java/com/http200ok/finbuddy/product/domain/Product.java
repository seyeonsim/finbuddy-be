package com.http200ok.finbuddy.product.domain;

import com.http200ok.finbuddy.bank.domain.Bank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "product_type", discriminatorType = DiscriminatorType.STRING)
@Check(constraints = "product_type IN ('DEPOSIT', 'SAVING', 'CHECKING')")
@Getter
@Setter
@NoArgsConstructor
public abstract class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    protected Bank bank;

    @Column(nullable = false)
    protected String code;

    @Column(nullable = false)
    protected String name;

    protected String subscriptionMethod;
    protected String maturityInterestRate;

    @Column(length = 500)
    protected String specialCondition;

    protected String subscriptionRestriction;
    protected String subscriptionTarget;
    protected String additionalNotes;
    protected Long maximumLimit;

    protected LocalDate disclosureSubmissionMonth; // 공시 제출월 (YYYYMM)
    protected LocalDate disclosureStartDate; // 공시 시작일
    protected LocalDate disclosureEndDate; // 공시 종료일
    protected LocalDateTime financialCompanySubmissionDate; // 금융회사 제출일 (YYYYMMDDHH24MI)
}