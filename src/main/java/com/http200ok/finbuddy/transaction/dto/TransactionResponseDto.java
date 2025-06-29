package com.http200ok.finbuddy.transaction.dto;

import com.http200ok.finbuddy.transaction.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponseDto {
    private Long transactionId;
    private String opponentName;
    private Integer transactionType; // 1: 입금, 2: 출금
    private Long amount;
    private Long updatedBalance;
    private LocalDateTime transactionDate;
    private String categoryName;

    public static TransactionResponseDto fromEntity(Transaction transaction) {
        return new TransactionResponseDto(
                transaction.getId(),
                transaction.getOpponentName(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getUpdatedBalance(),
                transaction.getTransactionDate(),
                transaction.getCategory().getName()
        );
    }
}
