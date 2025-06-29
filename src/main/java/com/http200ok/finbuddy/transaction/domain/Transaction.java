package com.http200ok.finbuddy.transaction.domain;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.category.domain.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String opponentName;

    @Column(nullable = false)
    private Integer transactionType; // 입금(1) or 출금(2)

    @Column(nullable = false)
    private Long amount;

    private Long updatedBalance;

    private LocalDateTime transactionDate;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 생성 메소드
    public static Transaction createTransaction(Account account, String opponentName, Long amount, Integer transactionType, Category category) {
        if (transactionType != 1 && transactionType != 2) {
            throw new IllegalArgumentException("유효하지 않은 거래 타입입니다. (1: 입금, 2: 출금)");
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setOpponentName(opponentName);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);

        // 계좌 잔액 업데이트
        long currentBalance = account.getBalance();
        if (transactionType == 1) { // 입금
            currentBalance += amount;
        } else { // 출금
            // 잔액이 부족한 경우 예외 발생
            if (currentBalance < amount) {
                throw new IllegalArgumentException("계좌 잔액이 부족합니다.");
            }
            currentBalance -= amount;
        }

        account.setBalance(currentBalance);
        transaction.setUpdatedBalance(currentBalance);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCategory(category);

        // 연관관계 설정
        account.getTransactions().add(transaction);

        return transaction;
    }

    // 더미 데이터용 생성 메서드
    public static Transaction createDummyTransaction(
            Account account, Integer transactionType, Long amount,
            Category category, LocalDateTime transactionDate,
            String opponentName, Long updatedBalance) {

        if (transactionType != 1 && transactionType != 2) {
            throw new IllegalArgumentException("유효하지 않은 거래 타입입니다. (1: 입금, 2: 출금)");
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setTransactionDate(transactionDate);
        transaction.setOpponentName(opponentName);
        transaction.setUpdatedBalance(updatedBalance);

        // 연관관계 설정
        account.getTransactions().add(transaction);

        return transaction;
    }
}
