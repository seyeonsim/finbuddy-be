package com.http200ok.finbuddy.account.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.dto.AccountResponseDto;
import com.http200ok.finbuddy.account.dto.AccountSummaryResponseDto;
import com.http200ok.finbuddy.account.dto.CheckingAccountsSummaryResponseDto;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.common.validator.AccountValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountValidator accountValidator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountResponseDto getAccountDetails(Long memberId, Long accountId) {
        // AccountValidator를 사용하여 계좌 권한 검증 및 조회
        Account account = accountValidator.validateAndGetAccount(accountId, memberId);

        return AccountResponseDto.from(account);
    }

    @Override
    public CheckingAccountsSummaryResponseDto getCheckingAccountsSummary(Long memberId) {
        // 모든 CheckingAccount 가져오기
        List<Account> checkingAccounts = accountRepository.findCheckingAccountsByMemberId(memberId);

        // 잔고 합계 계산
        Long totalBalance = checkingAccounts.stream()
                .mapToLong(Account::getBalance)
                .sum();

        // Checking 계좌 개수 계산
        int checkingAccountsCount = checkingAccounts.size();

        // 계좌 ID로 정렬된 리스트에서 상위 3개만 가져오기
        List<AccountSummaryResponseDto> top3Accounts = checkingAccounts.stream()
                .limit(3)
                .map(AccountSummaryResponseDto::from)
                .collect(Collectors.toList());

        return new CheckingAccountsSummaryResponseDto(totalBalance, checkingAccountsCount, top3Accounts);
    }

    @Override
    public List<AccountSummaryResponseDto> getAccountsByMemberId(Long memberId) {
        List<Account> accounts = accountRepository.findAccountsByMemberId(memberId);
        return accounts.stream()
                .map(AccountSummaryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 계좌 비밀번호 검증
     * 비밀번호 검증 후 로그인 실패 횟수를 업데이트하는 로직이 추가된다면, @Transactional이 적합
     */
    public boolean verifyPassword(Long accountId, String inputPassword) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("해당 계좌를 찾을 수 없습니다."));

        // 비밀번호 일치 여부 반환
        return passwordEncoder.matches(inputPassword, account.getPassword());
    }
}
