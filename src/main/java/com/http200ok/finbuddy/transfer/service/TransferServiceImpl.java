package com.http200ok.finbuddy.transfer.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.dto.CheckingAccountResponseDto;
import com.http200ok.finbuddy.account.dto.ReceivingAccountResponseDto;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.category.domain.Category;
import com.http200ok.finbuddy.category.repository.CategoryRepository;
import com.http200ok.finbuddy.common.exception.InsufficientBalanceException;
import com.http200ok.finbuddy.common.exception.InvalidTransactionException;
import com.http200ok.finbuddy.common.validator.AccountValidator;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<CheckingAccountResponseDto> getCheckingAccountList(Long memberId) {
        // 모든 CheckingAccount 가져오기
        List<Account> checkingAccounts = accountRepository.findCheckingAccountsByMemberId(memberId);

        return checkingAccounts.stream()
                .map(CheckingAccountResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReceivingAccountResponseDto getReceivingAccount(String bankName, String accountNumber) {
        Account account = accountValidator.validateAndGetBankAccount(bankName, accountNumber);
        return ReceivingAccountResponseDto.from(account);
    }

    /**
     * 이체 처리
     * @param fromAccountId 출금 계좌 ID
     * @param toBankName 입금 계좌 은행명
     * @param toAccountNumber 입금 계좌번호
     * @param amount 이체 금액
     * @param password 계좌 비밀번호
     * @param senderName 보내는 사람 이름, 받는 분 통장에 표시
     * @param receiverName 받는 사람 이름, 내 통상에 표시
     * @return 이체 성공 여부
     */
    @Override
    @Transactional
    public boolean executeAccountTransfer(Long memberId, Long fromAccountId, String toBankName, String toAccountNumber,
                                          Long amount, String password, String senderName, String receiverName) {

        // 출금 계좌 조회 및 검증 (비관적 락 사용)
        Account fromAccount = accountValidator.validateAndGetAccountWithLock(fromAccountId, memberId);

        // 입금 계좌 검증 및 조회 (비관적 락 사용)
        Account toAccount = accountValidator.validateAndGetBankAccountWithLock(toBankName, toAccountNumber);

        if (receiverName == null) {
            receiverName = toAccount.getMember().getName();
        }

        // 동일 계좌 검증
        if (fromAccount.getAccountNumber().equals(toAccountNumber) &&
                fromAccount.getBank().getName().equals(toBankName)) {
            throw new InvalidTransactionException("출금계좌와 입금계좌가 동일합니다");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, fromAccount.getPassword())) {
            throw new InvalidTransactionException("계좌 비밀번호가 일치하지 않습니다");
        }

        // 잔액 확인
        if (fromAccount.getBalance() < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다");
        }

        // 거래 카테고리 조회 (이체 카테고리 - 실제 코드에서는 상수로 관리하거나 DB에서 조회)
        Category transferCategory = categoryRepository.findById(7L) // 기타로 우선 저장
                .orElseThrow(() -> new EntityNotFoundException("거래 카테고리를 찾을 수 없습니다"));

        // 출금/입금 계좌 잔액 업데이트
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // 출금(2)/입금(1) 거래내역 생성
        Transaction withdrawalTransaction = Transaction.createTransaction(fromAccount, receiverName, amount, 2, transferCategory);
        Transaction depositTransaction = Transaction.createTransaction(toAccount, senderName, amount, 1, transferCategory);

        // 거래내역 저장
        transactionRepository.save(withdrawalTransaction);
        transactionRepository.save(depositTransaction);

        // 계좌 정보 업데이트
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return true;
    }

    @Override
    @Transactional
    public boolean autoExecuteAccountTransfer(Long memberId, Long fromAccountId, String toBankName, String toAccountNumber,
                                          Long amount, String senderName, String receiverName) {

        // 출금 계좌 조회 및 검증 (비관적 락 사용)
        Account fromAccount = accountValidator.validateAndGetAccountWithLock(fromAccountId, memberId);

        // 입금 계좌 검증 및 조회 (비관적 락 사용)
        Account toAccount = accountValidator.validateAndGetBankAccountWithLock(toBankName, toAccountNumber);

        if (receiverName == null) {
            receiverName = toAccount.getMember().getName();
        }

        // 동일 계좌 검증
        if (fromAccount.getAccountNumber().equals(toAccountNumber) &&
                fromAccount.getBank().getName().equals(toBankName)) {
            throw new InvalidTransactionException("출금계좌와 입금계좌가 동일합니다");
        }

        // 잔액 확인
        if (fromAccount.getBalance() < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다");
        }

        // 거래 카테고리 조회 (이체 카테고리 - 실제 코드에서는 상수로 관리하거나 DB에서 조회)
        Category transferCategory = categoryRepository.findById(7L) // 기타로 우선 저장
                .orElseThrow(() -> new EntityNotFoundException("거래 카테고리를 찾을 수 없습니다"));

        // 출금/입금 계좌 잔액 업데이트
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // 출금(2)/입금(1) 거래내역 생성
        Transaction withdrawalTransaction = Transaction.createTransaction(fromAccount, receiverName, amount, 2, transferCategory);
        Transaction depositTransaction = Transaction.createTransaction(toAccount, senderName, amount, 1, transferCategory);

        // 거래내역 저장
        transactionRepository.save(withdrawalTransaction);
        transactionRepository.save(depositTransaction);

        // 계좌 정보 업데이트
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return true;
    }
}
