package com.http200ok.finbuddy.product.repository;

import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.product.domain.DepositProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DepositProductRepository extends JpaRepository<DepositProduct, Long> {
    Optional<DepositProduct> findByNameAndBank(String name, Bank bank);
    Page<DepositProduct> findByNameContainingAndBank_NameContainingOrderByDisclosureStartDateDesc(String name, String bankName, Pageable pageable);

    @Query("""
        SELECT DISTINCT dp FROM DepositProduct dp
        JOIN dp.options o
        GROUP BY dp
        ORDER BY MAX(o.maximumInterestRate) DESC, dp.disclosureStartDate DESC
        """)
    List<DepositProduct> findTop3DepositProductsByMaxInterestRate(Pageable pageable);
}
