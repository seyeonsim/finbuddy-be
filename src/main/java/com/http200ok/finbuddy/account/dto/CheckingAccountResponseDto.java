package com.http200ok.finbuddy.account.dto;

import com.http200ok.finbuddy.account.domain.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckingAccountResponseDto {
    private Long accountId;
    private String bankName;
    private String senderName;
    private String accountName;
    private String accountNumber;
    private Long balance;

    public static CheckingAccountResponseDto from(Account account) {
        return new CheckingAccountResponseDto(
                account.getId(),
                account.getBank().getName(),
                account.getMember().getName(),
                account.getAccountName(),
                account.getAccountNumber(),
                account.getBalance()
        );
    }
}
