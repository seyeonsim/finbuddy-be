package com.http200ok.finbuddy.account.service;

import com.http200ok.finbuddy.account.dto.AccountResponseDto;
import com.http200ok.finbuddy.account.dto.AccountSummaryResponseDto;
import com.http200ok.finbuddy.account.dto.CheckingAccountsSummaryResponseDto;

import java.util.List;

public interface AccountService {
    AccountResponseDto getAccountDetails(Long memberId, Long accountId);
    CheckingAccountsSummaryResponseDto getCheckingAccountsSummary(Long memberId);
    List<AccountSummaryResponseDto> getAccountsByMemberId(Long memberId);
    boolean verifyPassword(Long accountId, String inputPassword);
}
