package com.http200ok.finbuddy.common.validator;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.common.exception.UnauthorizedAccessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountValidatorImpl implements AccountValidator{

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public Account validateAndGetAccount(Long accountId, Long memberId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));

        if (!account.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException("Member " + memberId + " is not authorized to access account " + accountId);
        }

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public Account validateAndGetBankAccount(String bankName, String accountNumber) {
        return accountRepository.findByBankNameAndAccountNumber(bankName, accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("계좌를 찾을 수 없거나 은행명과 계좌번호가 일치하지 않습니다"));
    }

    @Override
    public Account validateAndGetAccountWithLock(Long accountId, Long memberId) {
        // 비관적 락을 사용하여 계좌 조회
        Account account = accountRepository.findByIdWithPessimisticLock(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));

        if (!account.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException("Member " + memberId + " is not authorized to access account " + accountId);
        }

        return account;
    }

    @Override
    public Account validateAndGetBankAccountWithLock(String bankName, String accountNumber) {
        // 은행명과 계좌번호로 계좌를 한 번에 조회, 비관적 락 적용
        return accountRepository.findByBankNameAndAccountNumberWithPessimisticLock(bankName, accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("계좌를 찾을 수 없거나 은행명과 계좌번호가 일치하지 않습니다"));
    }

}
