package com.http200ok.finbuddy.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendedProductDto {
    private Long productId;
    private String productType; // 예금(DEPOSIT) 또는 적금(SAVING)
    private String name;
    private Long bankId;
    private String bankName;
    private String bankLogoUrl;
    private String interestRateTypeName;
    private Double minInterestRate;
    private Double maxInterestRate;
    private Integer minSavingTerm;
    private Integer maxSavingTerm;
}
