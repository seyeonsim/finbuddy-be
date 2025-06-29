package com.http200ok.finbuddy.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CheckingAccountsSummaryResponseDto {
    private Long totalBalance;
    private int checkingAccountsCount;
    private List<AccountSummaryResponseDto> top3Accounts;
}
