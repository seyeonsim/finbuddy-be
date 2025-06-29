package com.http200ok.finbuddy.account.dto;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.domain.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountSummaryResponseDto {
    private Long accountId;
    private String accountName;
    private AccountType accountType;
    private String bankLogoUrl;
    private String accountNumber;
    private Long balance;

    public static AccountSummaryResponseDto from(Account account) {
        return new AccountSummaryResponseDto(
                account.getId(),
                account.getAccountName(),
                account.getAccountType(),
                account.getBank().getLogoUrl(),
                account.getAccountNumber(),
                account.getBalance()
        );
    }
}
