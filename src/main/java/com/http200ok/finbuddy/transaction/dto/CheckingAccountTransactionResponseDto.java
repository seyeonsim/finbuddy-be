package com.http200ok.finbuddy.transaction.dto;

import com.http200ok.finbuddy.transaction.domain.Transaction;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CheckingAccountTransactionResponseDto {  // 이름 변경
    private String accountName;
    private String accountNumber;
    private String opponentName;
    private Integer transactionType; // 입금(1) or 출금(2)
    private Long amount;
    private Long updatedBalance;
    private LocalDateTime transactionDate;

    public CheckingAccountTransactionResponseDto(Transaction transaction) {
        this.accountName = transaction.getAccount().getAccountName();
        this.accountNumber = transaction.getAccount().getAccountNumber();
        this.opponentName = transaction.getOpponentName();
        this.transactionType = transaction.getTransactionType();
        this.amount = transaction.getAmount();
        this.updatedBalance = transaction.getUpdatedBalance();
        this.transactionDate = transaction.getTransactionDate();
    }
}
