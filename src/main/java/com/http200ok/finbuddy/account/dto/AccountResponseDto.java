package com.http200ok.finbuddy.account.dto;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.domain.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AccountResponseDto {

    private Long accountId;
    private String accountName;
    private String accountNumber;
    private AccountType accountType;
    private LocalDate createdAt;
    private LocalDate maturedAt; // 예·적금 계좌만 포함
    private Double interestRate; // 예·적금 계좌만 포함
    private String logoUrl;
    private Long bankId;
    private String bankName;

    public static AccountResponseDto from(Account account) {
        LocalDate createdAt = (account.getCreatedAt() != null) ? account.getCreatedAt().toLocalDate() : null;
        LocalDate maturedAt = (account.getMaturedAt() != null) ? account.getMaturedAt().toLocalDate() : null;
        Double interestRate = (account.getSelectedOption() != null) ? account.getSelectedOption().getInterestRate() : null;

        return new AccountResponseDto(
                account.getId(),
                account.getAccountName(),
                account.getAccountNumber(),
                account.getAccountType(),
                createdAt,   // LocalDate 변환
                maturedAt,   // LocalDate 변환 (예·적금 계좌만)
                interestRate, // 예·적금 계좌만
                account.getBank().getLogoUrl(),
                account.getBank().getId(),
                account.getBank().getName()
        );
    }
}
