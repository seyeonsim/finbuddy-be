package com.http200ok.finbuddy.product.dto;

import com.http200ok.finbuddy.product.domain.SavingProductOption;
import lombok.Getter;

@Getter
public class SavingProductOptionDto {
    private Long productOptionId;
    private String interestRateType;
    private String interestRateTypeName;
    private Integer savingTerm;
    private Double interestRate;
    private Double maximumInterestRate;
    private String reserveType;
    private String reserveTypeName;

    public SavingProductOptionDto(SavingProductOption option) {
        this.productOptionId = option.getId();
        this.interestRateType = option.getInterestRateType();
        this.interestRateTypeName = option.getInterestRateTypeName();
        this.savingTerm = option.getSavingTerm();
        this.interestRate = option.getInterestRate();
        this.maximumInterestRate = option.getMaximumInterestRate();
        this.reserveType = option.getReserveType();
        this.reserveTypeName = option.getReserveTypeName();
    }
}
