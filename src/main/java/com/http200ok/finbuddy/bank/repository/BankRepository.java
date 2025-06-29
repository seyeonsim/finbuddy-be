package com.http200ok.finbuddy.bank.repository;

import com.http200ok.finbuddy.bank.domain.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, Long> {
    Optional<Bank> findByCode(String code);

    Optional<Bank> findByName(String bankName);
}
