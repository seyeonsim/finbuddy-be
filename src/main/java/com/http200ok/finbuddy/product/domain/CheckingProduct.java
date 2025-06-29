package com.http200ok.finbuddy.product.domain;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.product.domain.DepositProductOption;
import com.http200ok.finbuddy.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CHECKING")
@Getter @Setter
@NoArgsConstructor
public class CheckingProduct extends Product {

    // 생성 메소드
    public static CheckingProduct createProduct(Bank bank, String code, String name) {
        CheckingProduct product = new CheckingProduct();
        product.bank = bank;
        product.code = code;
        product.name = name;
        return product;
    }
}