package com.http200ok.finbuddy.product.domain;

import lombok.Getter;

@Getter
public enum ProductType {
    DEPOSIT("deposit", "depositProductsSearch.json"),
    SAVING("saving", "savingProductsSearch.json");

    private final String typeName;
    private final String apiPath;

    ProductType(String typeName, String apiPath) {
        this.typeName = typeName;
        this.apiPath = apiPath;
    }

    public static ProductType from(String typeName) {
        for (ProductType type : values()) {
            if (type.typeName.equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 productType: " + typeName);
    }
}
