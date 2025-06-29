package com.http200ok.finbuddy.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "option_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_option_id")
    private Long id;

    @Column
    private String interestRateType; // 저축 금리 유형

    @Column
    private String interestRateTypeName; // 저축 금리 유형명

    @Column
    private Integer savingTerm; // 저축 기간 (개월 단위)

    @Column
    private Double interestRate; // 저축 금리

    @Column
    private Double maximumInterestRate; // 최고 우대 금리

}
