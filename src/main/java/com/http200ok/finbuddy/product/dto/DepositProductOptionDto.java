package com.http200ok.finbuddy.product.dto;

import com.http200ok.finbuddy.product.domain.DepositProductOption;
import lombok.Getter;

@Getter
public class DepositProductOptionDto {
    private Long productOptionId;
    private String interestRateType;
    private String interestRateTypeName;
    private Integer savingTerm;
    private Double interestRate;
    private Double maximumInterestRate;

    public DepositProductOptionDto(DepositProductOption option) {
        this.productOptionId = option.getId();
        this.interestRateType = option.getInterestRateType();
        this.interestRateTypeName = option.getInterestRateTypeName();
        this.savingTerm = option.getSavingTerm();
        this.interestRate = option.getInterestRate();
        this.maximumInterestRate = option.getMaximumInterestRate();
    }
}
