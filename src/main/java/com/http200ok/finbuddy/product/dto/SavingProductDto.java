package com.http200ok.finbuddy.product.dto;

import com.http200ok.finbuddy.product.domain.SavingProduct;
import com.http200ok.finbuddy.product.domain.SubscriptionRestriction;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SavingProductDto {
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
    private List<SavingProductOptionDto> options;

    public SavingProductDto(SavingProduct savingProduct) {
        this.productId = savingProduct.getId();
        this.bankName = savingProduct.getBank().getName();
        this.bankLogoUrl = savingProduct.getBank().getLogoUrl();
        this.name = savingProduct.getName();
        this.subscriptionMethod = savingProduct.getSubscriptionMethod();
        this.maturityInterestRate = savingProduct.getMaturityInterestRate();
        this.specialCondition = savingProduct.getSpecialCondition();

        this.subscriptionRestriction = SubscriptionRestriction.getDescriptionByCode(
                Integer.parseInt(savingProduct.getSubscriptionRestriction())
        );

        this.subscriptionTarget = savingProduct.getSubscriptionTarget();
        this.additionalNotes = savingProduct.getAdditionalNotes();
        this.maximumLimit = savingProduct.getMaximumLimit();
        this.options = savingProduct.getOptions().stream()
                .map(SavingProductOptionDto::new)
                .collect(Collectors.toList());
    }
}
