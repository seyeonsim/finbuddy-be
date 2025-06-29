package com.http200ok.finbuddy.transaction.repository;

import com.http200ok.finbuddy.category.dto.CategoryExpenseDto;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 특정 유저의 입출금 계좌에 한하여 거래 내역 최신순으로 반환
    @Query("""
        SELECT t FROM Transaction t
        JOIN t.account a
        JOIN a.member m
        WHERE m.id = :memberId
        AND a.accountType = 'CHECKING'
        ORDER BY t.transactionDate DESC
    """)
    Page<Transaction> findLatestTransactionsForUserCheckingAccounts(@Param("memberId") Long memberId, Pageable pageable);

    // 특정 유저의 입출금 계좌에 한하여 당월 출금 내역 합산
    @Query("""
    SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
    JOIN t.account a
    JOIN a.member m
    WHERE m.id = :memberId
    AND a.accountType = 'CHECKING'
    AND t.transactionType = 2
    AND FUNCTION('YEAR', t.transactionDate) = FUNCTION('YEAR', CURRENT_DATE)
    AND FUNCTION('MONTH', t.transactionDate) = FUNCTION('MONTH', CURRENT_DATE)
    """)
    Long getTotalSpendingForCurrentMonth(@Param("memberId") Long memberId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId"
            + " AND (:startDate IS NULL OR t.transactionDate >= :startDate)"
            + " AND (:endDate IS NULL OR t.transactionDate <= :endDate)"
            + " AND (:transactionType IS NULL OR t.transactionType = :transactionType)")
    Page<Transaction> findTransactions(@Param("accountId") Long accountId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("transactionType") Integer transactionType,
                                       Pageable pageable);

    // 특정 연-월, 특정 memberId의 Checking 계좌 트랜잭션 조회
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.transactionType = 2
            AND t.account.member.id = :memberId
            AND t.account.accountType = 'CHECKING'
            AND FUNCTION('YEAR', t.transactionDate) = :year
            AND FUNCTION('MONTH', t.transactionDate) = :month
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findTransactionByYearMonthForCheckingAccounts(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 연-월의 Checking 계좌에서 카테고리별 거래 금액 합계 조회
    @Query("""
            SELECT NEW com.http200ok.finbuddy.category.dto.CategoryExpenseDto(
                t.category.name,
                CAST(COALESCE(SUM(t.amount), 0) AS long),
                0.0)
            FROM Transaction t
            JOIN t.account a
            JOIN a.member m
            WHERE t.transactionType = 2
            AND m.id = :memberId
            AND a.accountType = 'CHECKING'
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            GROUP BY t.category.name
            """)
    List<CategoryExpenseDto> sumAmountByCategoryForMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );


    // 특정 연-월의 Checking 계좌에서 전체 amount 합계 조회
    @Query("""
            SELECT CAST(COALESCE(SUM(t.amount), 0) AS long)
            FROM Transaction t
            WHERE t.transactionType = 2
            AND t.account.member.id = :memberId
            AND t.account.accountType = 'CHECKING'
            AND FUNCTION('YEAR', t.transactionDate) = :year
            AND FUNCTION('MONTH', t.transactionDate) = :month
            """)
    Long sumTotalAmountForMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );


    // 특정 연-월 계좌 별로 전체 출금 amount 합계 조회
    @Query("""
            SELECT CAST(COALESCE(SUM(t.amount), 0) AS long)
            FROM Transaction t
            WHERE t.transactionType = 2
            AND t.account.member.id = :memberId
            AND t.account.id = :accountId
            AND FUNCTION('YEAR', t.transactionDate) = :year
            AND FUNCTION('MONTH', t.transactionDate) = :month
            """)
    Long getTotalSpendingForMonth(
            @Param("memberId") Long memberId,
            @Param("accountId") Long accountId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 연-월 계좌 별로 전체 입금 amount 합계 조회
    @Query("""
            SELECT CAST(COALESCE(SUM(t.amount), 0) AS long)
            FROM Transaction t
            WHERE t.transactionType = 1
            AND t.account.member.id = :memberId
            AND t.account.id = :accountId
            AND FUNCTION('YEAR', t.transactionDate) = :year
            AND FUNCTION('MONTH', t.transactionDate) = :month
            """)
    Long getTotalIncomeForMonth(
            @Param("memberId") Long memberId,
            @Param("accountId") Long accountId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 연-월의 계좌에서 카테고리별 거래 금액 합계 조회
    @Query("""
            SELECT NEW com.http200ok.finbuddy.category.dto.CategoryExpenseDto(
                t.category.name,
                CAST(COALESCE(SUM(t.amount), 0) AS long),
                0.0)
            FROM Transaction t
            JOIN t.account a
            JOIN a.member m
            WHERE t.transactionType = 2
            AND m.id = :memberId
            AND a.id = :accountId
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            GROUP BY t.category.name
            """)
    List<CategoryExpenseDto> getTotalSpendingByCategoryForMonth(
            @Param("memberId") Long memberId,
            @Param("accountId") Long accountId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
        SELECT t FROM Transaction t
        JOIN t.account a
        JOIN a.member m
        WHERE m.id = :memberId
        AND a.accountType = 'CHECKING'
        AND FUNCTION('YEAR', t.transactionDate) = :year
        AND FUNCTION('MONTH', t.transactionDate) = :month
        ORDER BY t.transactionDate DESC
    """)
    List<Transaction> findLatestTransactionsForUserCheckingAccountsInMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );

    List<Transaction> findByAccountId(Long id);

    int deleteByAccountId(Long id);
}
