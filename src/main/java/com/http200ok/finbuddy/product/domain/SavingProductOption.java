package com.http200ok.finbuddy.product.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("SAVING")
@Getter @Setter
@NoArgsConstructor
public class SavingProductOption extends ProductOption {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private SavingProduct savingProduct;

    @Column
    private String reserveType;

    @Column
    private String reserveTypeName;

    public static SavingProductOption createSavingProductOption(SavingProduct savingProduct,
                                                                String interestRateType,
                                                                String interestRateTypeName,
                                                                Integer savingTerm,
                                                                Double interestRate,
                                                                Double maximumInterestRate,
                                                                String reserveType,
                                                                String reserveTypeName) {
        SavingProductOption option = new SavingProductOption();
        option.setSavingProduct(savingProduct);
        option.setInterestRateType(interestRateType);
        option.setInterestRateTypeName(interestRateTypeName);
        option.setSavingTerm(savingTerm);
        option.setInterestRate(interestRate);
        option.setMaximumInterestRate(maximumInterestRate);
        option.setReserveType(reserveType);
        option.setReserveTypeName(reserveTypeName);
        return option;
    }
}
