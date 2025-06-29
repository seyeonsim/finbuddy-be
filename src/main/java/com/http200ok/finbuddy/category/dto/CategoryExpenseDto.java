package com.http200ok.finbuddy.category.dto;

import lombok.Getter;

@Getter
//@NoArgsConstructor
//@AllArgsConstructor
public class CategoryExpenseDto {
    private String categoryName;
    private Long totalAmount;
    private Double percentage;

    public CategoryExpenseDto(String categoryName, Long totalAmount, Double percentage) {
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
    }
}
