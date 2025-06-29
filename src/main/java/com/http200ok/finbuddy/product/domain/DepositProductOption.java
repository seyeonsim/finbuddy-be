package com.http200ok.finbuddy.product.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("DEPOSIT")
@Getter @Setter
@NoArgsConstructor
public class DepositProductOption extends ProductOption {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private DepositProduct depositProduct;

    public static DepositProductOption createDepositProductOption(DepositProduct depositProduct,
                                                                  String interestRateType,
                                                                  String interestRateTypeName,
                                                                  Integer savingTerm,
                                                                  Double interestRate,
                                                                  Double maximumInterestRate) {
        DepositProductOption option = new DepositProductOption();
        option.setDepositProduct(depositProduct);
        option.setInterestRateType(interestRateType);
        option.setInterestRateTypeName(interestRateTypeName);
        option.setSavingTerm(savingTerm);
        option.setInterestRate(interestRate);
        option.setMaximumInterestRate(maximumInterestRate);
        return option;
    }
}

