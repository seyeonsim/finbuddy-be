package com.http200ok.finbuddy.transaction.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.category.domain.Category;
import com.http200ok.finbuddy.category.repository.CategoryRepository;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 계좌 간 거래 및 거래내역 생성 서비스
 * - 계좌 간 이체 처리
 * - 적금/예금 납입 처리
 * - 거래내역 단일 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTransactionServiceImpl implements AccountTransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionFixService transactionFixService;

    private static final int INCOME_TYPE = 1;
    private static final int EXPENSE_TYPE = 2;
    private static final Long TRANSFER_CATEGORY_ID = 7L; // 이체 카테고리 ID

    /**
     * 두 계좌 간 이체 처리 (출금 계좌에서 입금 계좌로)
     * 적금/예금 납입의 경우에도 사용
     * 이체 거래내역은 출금 계좌에만 생성하고 입금 계좌에는 생성하지 않음
     */
    @Transactional
    public void transferBetweenAccounts(Account fromAccount, Account toAccount,
                                        long amount, String description) {
        // 카테고리 조회 (이체 카테고리)
        Category transferCategory = categoryRepository.findById(TRANSFER_CATEGORY_ID)
                .orElseThrow(() -> new RuntimeException("이체 카테고리를 찾을 수 없습니다: " + TRANSFER_CATEGORY_ID));

        // 현재 출금 계좌 잔액 확인
        long fromAccountBalance = fromAccount.getBalance();

        // 출금 가능 여부 확인
        if (fromAccountBalance < amount) {
            throw new RuntimeException("출금 계좌 잔액이 부족합니다. 현재 잔액: " + fromAccountBalance + ", 이체 금액: " + amount);
        }

        // 출금 계좌 잔액 감소
        fromAccount.setBalance(fromAccountBalance - amount);
        accountRepository.save(fromAccount);

        // 입금 계좌 잔액 증가
        long toAccountBalance = toAccount.getBalance();
        toAccount.setBalance(toAccountBalance + amount);
        accountRepository.save(toAccount);

        // 이체 설명에 입금 계좌 정보 포함
        String fullDescription = description + " (" + toAccount.getAccountName() + ")";

        // 출금 계좌에만 거래내역 생성 (이체 출금)
        Transaction outTransaction = Transaction.createDummyTransaction(
                fromAccount,
                EXPENSE_TYPE,
                amount,
                transferCategory,
                LocalDateTime.now(),
                fullDescription,
                fromAccountBalance - amount
        );

        transactionRepository.save(outTransaction);

        log.info("계좌 간 이체 완료: {}에서 {}로 {}원 이체됨",
                fromAccount.getAccountName(), toAccount.getAccountName(), amount);
    }

    /**
     * 적금 납입 처리 (메인 계좌에서 적금 계좌로)
     */
    @Transactional
    public void processSavingDeposit(Account mainAccount, Account savingAccount, long amount) {
        // 적금 납입 이체 처리
        transferBetweenAccounts(
                mainAccount,
                savingAccount,
                amount,
                savingAccount.getAccountName() + " 적금 납입"
        );
    }

    /**
     * 예금 입금 처리 (메인 계좌에서 예금 계좌로)
     */
    @Transactional
    public void processDepositTransfer(Account mainAccount, Account depositAccount, long amount) {
        // 예금 이체 처리
        transferBetweenAccounts(
                mainAccount,
                depositAccount,
                amount,
                depositAccount.getAccountName() + " 예금 가입"
        );
    }

    /**
     * 일반 이체 처리 (한 계좌에서 다른 계좌로)
     */
    @Transactional
    public void processGeneralTransfer(Account fromAccount, Account toAccount, long amount, String description) {
        // 일반 이체 처리
        transferBetweenAccounts(
                fromAccount,
                toAccount,
                amount,
                description != null ? description : "계좌 이체"
        );
    }

    /**
     * 단일 거래내역 생성 (입금 또는 출금)
     */
    @Transactional
    public Transaction createSingleTransaction(Account account, int transactionType,
                                               long amount, Category category,
                                               LocalDateTime transactionDate, String description) {
        // 계좌 잔액 확인 및 업데이트
        long currentBalance = account.getBalance();
        long newBalance;

        if (transactionType == INCOME_TYPE) {
            newBalance = currentBalance + amount;
        } else if (transactionType == EXPENSE_TYPE) {
            // 출금 가능 여부 확인
            if (currentBalance < amount) {
                throw new RuntimeException("계좌 잔액이 부족합니다. 현재 잔액: " + currentBalance + ", 출금 금액: " + amount);
            }
            newBalance = currentBalance - amount;
        } else {
            throw new IllegalArgumentException("잘못된 거래 유형: " + transactionType);
        }

        // 계좌 잔액 업데이트
        account.setBalance(newBalance);
        accountRepository.save(account);

        // 거래내역 생성
        Transaction transaction = Transaction.createDummyTransaction(
                account,
                transactionType,
                amount,
                category,
                transactionDate != null ? transactionDate : LocalDateTime.now(),
                description,
                newBalance
        );

        return transactionRepository.save(transaction);
    }
}