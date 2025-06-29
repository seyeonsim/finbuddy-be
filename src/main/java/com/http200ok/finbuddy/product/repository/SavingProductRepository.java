package com.http200ok.finbuddy.product.repository;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.product.domain.SavingProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SavingProductRepository extends JpaRepository<SavingProduct, Long> {
    Optional<SavingProduct> findByNameAndBank(String name, Bank bank);
    Page<SavingProduct> findByNameContainingAndBank_NameContainingOrderByDisclosureStartDateDesc(String name, String bankName, Pageable pageable);

    @Query("""
        SELECT sp FROM SavingProduct sp
        JOIN sp.options o
        GROUP BY sp
        ORDER BY MAX(o.maximumInterestRate) DESC, sp.disclosureStartDate DESC
    """)
    List<SavingProduct> findTop3SavingProductsByMaxInterestRate(Pageable pageable);

}
