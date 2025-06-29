package com.http200ok.finbuddy.account.repository;

import com.http200ok.finbuddy.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("""
        SELECT a FROM Account a 
        WHERE a.member.id = :memberId 
        ORDER BY a.id
    """)
    List<Account> findAccountsByMemberId(@Param("memberId") Long memberId);

    @Query("""
        SELECT a FROM Account a 
        WHERE a.member.id = :memberId 
        AND a.accountType = 'CHECKING' 
        ORDER BY a.id
    """)
    List<Account> findCheckingAccountsByMemberId(@Param("memberId") Long memberId);

    // ID로 조회하는 비관적 락 메소드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithPessimisticLock(@Param("id") Long id);

    // 은행명과 계좌번호로 조회하는 비관적 락 메소드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a JOIN a.bank b WHERE b.name = :bankName AND a.accountNumber = :accountNumber")
    Optional<Account> findByBankNameAndAccountNumberWithPessimisticLock(@Param("bankName") String bankName,
                                                                        @Param("accountNumber") String accountNumber);

    // 은행명과 계좌번호로 조회
    @Query("SELECT a FROM Account a JOIN a.bank b WHERE b.name = :bankName AND a.accountNumber = :accountNumber")
    Optional<Account> findByBankNameAndAccountNumber(@Param("bankName") String bankName, @Param("accountNumber") String accountNumber);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithPessimisticLock(@Param("accountNumber") String accountNumber);

    // 계좌번호로 계좌 조회
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}

