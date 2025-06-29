package com.http200ok.finbuddy.account.dto;

import com.http200ok.finbuddy.account.domain.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReceivingAccountResponseDto {
    private Long accountId;
    private String bankName;
    private String receiverName;
    private String accountNumber;

    public static ReceivingAccountResponseDto from(Account account) {
        return new ReceivingAccountResponseDto(
                account.getId(),
                account.getBank().getName(),
                account.getMember().getName(),
                account.getAccountNumber()
        );
    }
}
