package com.http200ok.finbuddy.product.dto;

import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.SubscriptionRestriction;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class DepositProductDto {
    private Long productId;
    private String bankName;
    private String bankLogoUrl;
    private String name;
    private String subscriptionMethod;
    private String maturityInterestRate;
    private String specialCondition;
    private String subscriptionRestriction;
    private String subscriptionTarget;
    private String additionalNotes;
    private Long maximumLimit;
    private List<DepositProductOptionDto> options; // 옵션 리스트

    public DepositProductDto(DepositProduct depositProduct) {
        this.productId = depositProduct.getId();
        this.bankName = depositProduct.getBank().getName();
        this.bankLogoUrl = depositProduct.getBank().getLogoUrl();
        this.name = depositProduct.getName();
        this.subscriptionMethod = depositProduct.getSubscriptionMethod();
        this.maturityInterestRate = depositProduct.getMaturityInterestRate();
        this.specialCondition = depositProduct.getSpecialCondition();

        this.subscriptionRestriction = SubscriptionRestriction.getDescriptionByCode(
                Integer.parseInt(depositProduct.getSubscriptionRestriction())
        );

        this.subscriptionTarget = depositProduct.getSubscriptionTarget();
        this.additionalNotes = depositProduct.getAdditionalNotes();
        this.maximumLimit = depositProduct.getMaximumLimit();
        this.options = depositProduct.getOptions().stream()
                .map(DepositProductOptionDto::new)
                .collect(Collectors.toList());
    }
}
