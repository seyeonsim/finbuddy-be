package com.http200ok.finbuddy.product.dto;

import com.http200ok.finbuddy.product.domain.DepositProduct;
import com.http200ok.finbuddy.product.domain.Product;
import com.http200ok.finbuddy.product.domain.ProductOption;
import com.http200ok.finbuddy.product.domain.SavingProduct;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductDto {
    private Long productId;
    private String bankName;
    private String bankLogoUrl;
    private String productName;
    private String subscriptionMethod;
    private String additionalNotes;
    private Double minInterestRate;
    private Double maxInterestRate;

    public ProductDto(Product product) {
        this.productId = product.getId();
        this.bankName = product.getBank().getName();
        this.bankLogoUrl = product.getBank().getLogoUrl();
        this.productName = product.getName();
        this.subscriptionMethod = product.getSubscriptionMethod();
        this.additionalNotes = product.getAdditionalNotes();

        // 상품 옵션에서 최소 및 최대 금리 설정
        List<? extends ProductOption> options = product instanceof DepositProduct
                ? ((DepositProduct) product).getOptions()
                : ((SavingProduct) product).getOptions();

        this.minInterestRate = options.stream()
                .mapToDouble(ProductOption::getInterestRate)
                .min().orElse(0.0);

        this.maxInterestRate = options.stream()
                .mapToDouble(ProductOption::getMaximumInterestRate)
                .max().orElse(0.0);
    }
}
