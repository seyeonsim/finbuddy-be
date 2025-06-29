package com.http200ok.finbuddy.common.validator;

import com.http200ok.finbuddy.account.domain.Account;

public interface AccountValidator {
    // 계좌 ID와 멤버 ID를 검증하여 계좌 소유권을 확인
    Account validateAndGetAccount(Long accountId, Long memberId);
    // 은행명과 계좌번호 검증
    Account validateAndGetBankAccount(String bankName, String accountNumber);
    // 계좌 ID와 멤버 ID 검증 - 비관적 락 사용
    Account validateAndGetAccountWithLock(Long accountId, Long memberId);
    // 은행명과 계좌번호 검증 - 비관적 락 사용
    Account validateAndGetBankAccountWithLock(String bankName, String accountNumber);
}
