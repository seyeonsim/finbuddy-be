package com.http200ok.finbuddy.product.domain;


import com.http200ok.finbuddy.bank.domain.Bank;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("DEPOSIT")
@Getter @Setter
@NoArgsConstructor
public class DepositProduct extends Product {

    @OneToMany(mappedBy = "depositProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepositProductOption> options = new ArrayList<>();

    // 연관관계 메소드
    public void addOption(DepositProductOption option) {
        options.add(option);
        option.setDepositProduct(this);
    }

    // 생성 메소드
    public static DepositProduct createProduct(Bank bank, String code, String name, String subscriptionMethod,
                                               String maturityInterestRate, String specialCondition,
                                               String subscriptionRestriction, String subscriptionTarget,
                                               String additionalNotes, Long maximumLimit,
                                               LocalDate disclosureSubmissionMonth, LocalDate disclosureStartDate,
                                               LocalDate disclosureEndDate, LocalDateTime financialCompanySubmissionDate) {
        DepositProduct product = new DepositProduct();
        product.bank = bank;
        product.code = code;
        product.name = name;
        product.subscriptionMethod = subscriptionMethod;
        product.maturityInterestRate = maturityInterestRate;
        product.specialCondition = specialCondition;
        product.subscriptionRestriction = subscriptionRestriction;
        product.subscriptionTarget = subscriptionTarget;
        product.additionalNotes = additionalNotes;
        product.maximumLimit = maximumLimit;
        product.disclosureSubmissionMonth = disclosureSubmissionMonth;
        product.disclosureStartDate = disclosureStartDate;
        product.disclosureEndDate = disclosureEndDate;
        product.financialCompanySubmissionDate = financialCompanySubmissionDate;
        return product;
    }
}
