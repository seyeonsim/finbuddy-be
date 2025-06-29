package com.http200ok.finbuddy.transaction.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.domain.AccountType;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;


/**
 * 거래내역 수정 전용 서비스
 * - 거래내역 중복 제거
 * - 잔액 재계산
 * - 거래내역-계좌 잔액 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionFixServiceImpl implements TransactionFixService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final int INCOME_TYPE = 1;
    private static final int EXPENSE_TYPE = 2;

    /**
     * 모든 계좌의 거래내역 검증 및 수정
     */
    @Transactional
    public void processAllAccounts() {
        List<Account> allAccounts = accountRepository.findAll();
        log.info("전체 {} 개 계좌의 거래내역 처리 시작", allAccounts.size());

        // 회원별로 계좌 그룹화
        Map<Long, List<Account>> memberAccounts = allAccounts.stream()
                .collect(Collectors.groupingBy(account -> account.getMember().getId()));

        // 각 회원별로 계좌 처리
        for (Map.Entry<Long, List<Account>> entry : memberAccounts.entrySet()) {
            Long memberId = entry.getKey();
            List<Account> accounts = entry.getValue();

            try {
                log.info("회원 ID {}: {} 개 계좌 처리 시작", memberId, accounts.size());
                processAccountsForMember(memberId, accounts);
                log.info("회원 ID {}: 모든 계좌 처리 완료", memberId);
            } catch (Exception e) {
                log.error("회원 ID {} 계좌 처리 중 오류: {}", memberId, e.getMessage(), e);
            }
        }

        log.info("전체 계좌 거래내역 처리 완료");
    }

    /**
     * 회원의 모든 계좌를 처리하는 메서드
     * - 각 계좌 유형별로 분류하여 처리
     * - 계좌 간 이체 관계 확인 및 처리
     */
    @Transactional
    public void processAccountsForMember(Long memberId, List<Account> accounts) {
        // 1. 계좌 유형별 분류
        List<Account> checkingAccounts = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.CHECKING)
                .collect(Collectors.toList());

        List<Account> depositAccounts = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.DEPOSIT)
                .collect(Collectors.toList());

        List<Account> savingAccounts = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.SAVING)
                .collect(Collectors.toList());

        log.info("회원 ID {}: 입출금 계좌 {}, 예금 계좌 {}, 적금 계좌 {}",
                memberId, checkingAccounts.size(), depositAccounts.size(), savingAccounts.size());

        // 2. 입출금 계좌 먼저 정리 (이체 출금 계좌가 먼저 정리되어야 함)
        for (Account account : checkingAccounts) {
            try {
                processAccountTransactions(account);
            } catch (Exception e) {
                log.error("입출금 계좌 ID {} 처리 중 오류: {}", account.getId(), e.getMessage(), e);
            }
        }

        // 3. 예금/적금 계좌 거래내역 확인 및 생성
        processDepositSavingAccounts(checkingAccounts, depositAccounts, savingAccounts);
    }

    /**
     * 예금/적금 계좌의 거래내역 확인 및 처리
     * - 입출금 계좌에서 이체된 거래에 대응하는 입금 거래 생성
     */
    private void processDepositSavingAccounts(List<Account> checkingAccounts,
                                              List<Account> depositAccounts,
                                              List<Account> savingAccounts) {
        // 메인 계좌 (첫 번째 입출금 계좌) 식별
        if (checkingAccounts.isEmpty()) {
            log.warn("입출금 계좌가 없어 예금/적금 계좌 처리를 건너뜁니다.");
            return;
        }

        Account mainAccount = checkingAccounts.get(0);

        // 1. 예금 계좌 처리
        for (Account depositAccount : depositAccounts) {
            try {
                // 먼저 현재 거래내역 확인
                List<Transaction> existingTransactions = transactionRepository.findByAccountId(depositAccount.getId());
                if (!existingTransactions.isEmpty()) {
                    // 이미 거래내역이 있으면 정리만 수행
                    processAccountTransactions(depositAccount);
                    continue;
                }

                // 메인 계좌에서 이 예금계좌로의 이체 거래 찾기
                List<Transaction> mainAccountTransactions = transactionRepository.findByAccountId(mainAccount.getId());

                // 예금 계좌 이름이 포함된 출금 거래 찾기
                Optional<Transaction> depositTransferOpt = mainAccountTransactions.stream()
                        .filter(tx -> tx.getTransactionType() == EXPENSE_TYPE
                                && tx.getOpponentName() != null
                                && tx.getOpponentName().contains(depositAccount.getAccountName())
                                && tx.getAmount() > 0)
                        .findFirst();

                if (depositTransferOpt.isPresent()) {
                    Transaction depositTransfer = depositTransferOpt.get();

                    // 예금 계좌에 입금 거래 생성
                    Transaction inTransaction = Transaction.createDummyTransaction(
                            depositAccount,
                            INCOME_TYPE,
                            depositTransfer.getAmount(),
                            depositTransfer.getCategory(),
                            depositTransfer.getTransactionDate(),
                            "예금 가입",
                            depositTransfer.getAmount() // 초기 잔액은 입금액과 동일
                    );

                    transactionRepository.save(inTransaction);
                    log.info("예금 계좌 ID {}: 입금 거래 생성됨 ({}원)",
                            depositAccount.getId(), depositTransfer.getAmount());

                    // 이자 거래 찾아서 예금 계좌에 추가
                    processInterestTransactions(depositAccount);
                } else {
                    log.warn("예금 계좌 ID {}: 메인 계좌에서 해당 예금으로의 이체 거래를 찾을 수 없습니다", depositAccount.getId());
                }

                // 거래내역 정리
                processAccountTransactions(depositAccount);
            } catch (Exception e) {
                log.error("예금 계좌 ID {} 처리 중 오류: {}", depositAccount.getId(), e.getMessage(), e);
            }
        }

        // 2. 적금 계좌 처리
        for (Account savingAccount : savingAccounts) {
            try {
                // 먼저 현재 거래내역 확인
                List<Transaction> existingTransactions = transactionRepository.findByAccountId(savingAccount.getId());
                if (!existingTransactions.isEmpty()) {
                    // 이미 거래내역이 있으면 정리만 수행
                    processAccountTransactions(savingAccount);
                    continue;
                }

                // 메인 계좌에서 이 적금계좌로의 이체 거래들 찾기
                List<Transaction> mainAccountTransactions = transactionRepository.findByAccountId(mainAccount.getId());

                // 적금 계좌 이름이 포함된 출금 거래들 찾기
                List<Transaction> savingTransfers = mainAccountTransactions.stream()
                        .filter(tx -> tx.getTransactionType() == EXPENSE_TYPE
                                && tx.getOpponentName() != null
                                && tx.getOpponentName().contains(savingAccount.getAccountName())
                                && tx.getAmount() > 0)
                        .collect(Collectors.toList());

                if (!savingTransfers.isEmpty()) {
                    // 날짜순 정렬
                    savingTransfers.sort(Comparator.comparing(Transaction::getTransactionDate));

                    // 각 이체에 대한 입금 거래 생성
                    long runningBalance = 0;
                    for (int i = 0; i < savingTransfers.size(); i++) {
                        Transaction savingTransfer = savingTransfers.get(i);
                        long amount = savingTransfer.getAmount();
                        runningBalance += amount;

                        // 적금 계좌에 입금 거래 생성
                        Transaction inTransaction = Transaction.createDummyTransaction(
                                savingAccount,
                                INCOME_TYPE,
                                amount,
                                savingTransfer.getCategory(),
                                savingTransfer.getTransactionDate(),
                                (i+1) + "회차 적금 납입",
                                runningBalance
                        );

                        transactionRepository.save(inTransaction);
                    }

                    log.info("적금 계좌 ID {}: {} 건의 입금 거래 생성됨",
                            savingAccount.getId(), savingTransfers.size());
                } else {
                    log.warn("적금 계좌 ID {}: 메인 계좌에서 해당 적금으로의 이체 거래를 찾을 수 없습니다", savingAccount.getId());
                }

                // 거래내역 정리
                processAccountTransactions(savingAccount);
            } catch (Exception e) {
                log.error("적금 계좌 ID {} 처리 중 오류: {}", savingAccount.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 예금 계좌의 이자 거래 처리
     */
    private void processInterestTransactions(Account depositAccount) {
        try {
            // 계좌 정보에서 생성일 확인
            LocalDateTime createdAt = depositAccount.getCreatedAt();
            if (createdAt == null) {
                log.warn("예금 계좌 ID {}: 생성일이 없어 이자 거래를 생성할 수 없습니다", depositAccount.getId());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime interestDate = createdAt.plusMonths(3);
            int interestCount = 0;
            long accountBalance = depositAccount.getBalance();

            while (interestDate.isBefore(now) && interestCount < 3) {
                // 이자율: 연 2~5% 정도로 계산
                double annualRate = 2.0 + (Math.random() * 3.0);
                double monthlyRate = annualRate / 12.0 / 100.0;
                long interestAmount = (long)(accountBalance * monthlyRate * 3); // 3개월 이자

                // 이자가 너무 작으면 조정 (최소 1만원)
                interestAmount = Math.max(interestAmount, 10000);
                accountBalance += interestAmount;

                // 이자 입금 거래 생성
                Transaction interestTx = Transaction.createDummyTransaction(
                        depositAccount,
                        INCOME_TYPE,
                        interestAmount,
                        null, // 이자에는 카테고리 없음
                        interestDate,
                        String.format("분기 이자 (연 %.1f%%)", annualRate),
                        accountBalance
                );

                transactionRepository.save(interestTx);
                log.info("예금 계좌 ID {}: {}에 이자 {}원 추가됨",
                        depositAccount.getId(), interestDate, interestAmount);

                interestDate = interestDate.plusMonths(3);
                interestCount++;
            }
        } catch (Exception e) {
            log.error("예금 계좌 ID {} 이자 처리 중 오류: {}", depositAccount.getId(), e.getMessage(), e);
        }
    }

    /**
     * 특정 계좌의 거래내역 검증 및 수정
     * - 중복 거래 제거
     * - 잔액 마이너스 보정
     * - 계좌 잔액과 마지막 거래 잔액 동기화
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAccountTransactions(Account account) {
        try {
            Long accountId = account.getId();
            String accountName = account.getAccountName();
            log.info("계좌 {} (ID: {}) 거래내역 처리 시작 (현재 계좌 잔액: {}원)",
                    accountName, accountId, account.getBalance());

            // 모든 거래내역 조회
            List<Transaction> allTransactions = transactionRepository.findByAccountId(accountId);

            if (allTransactions.isEmpty()) {
                log.info("계좌 {} (ID: {}): 거래내역 없음", accountName, accountId);
                return;
            }

            log.info("계좌 {} (ID: {}): 총 {}개 거래내역 로드됨", accountName, accountId, allTransactions.size());

            // 1. 중복 거래내역 제거
            List<Transaction> uniqueTransactions = removeDuplicateTransactions(allTransactions);

            if (uniqueTransactions.size() < allTransactions.size()) {
                log.info("계좌 {} (ID: {}): 중복 거래 {}개 제거됨 ({} -> {})",
                        accountName, accountId,
                        allTransactions.size() - uniqueTransactions.size(),
                        allTransactions.size(),
                        uniqueTransactions.size());
            }

            // 2. 거래 날짜 기준 정렬
            uniqueTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 첫 거래 확인
            if (!uniqueTransactions.isEmpty()) {
                Transaction firstTx = uniqueTransactions.get(0);
                log.info("계좌 {} (ID: {}): 첫 거래 - {} {}원, 날짜: {}, 설명: {}",
                        accountName, accountId,
                        firstTx.getTransactionType() == INCOME_TYPE ? "입금" : "출금",
                        firstTx.getAmount(),
                        firstTx.getTransactionDate(),
                        firstTx.getOpponentName());

                // 첫 거래가 출금인지 확인
                if (firstTx.getTransactionType() == EXPENSE_TYPE) {
                    log.warn("계좌 {} (ID: {}): 첫 거래가 출금입니다. 초기 입금이 필요합니다.", accountName, accountId);
                }
            }

            // 3. 기존 거래내역 모두 삭제 (완전히 새로 생성하기 위해)
            int deletedCount = transactionRepository.deleteByAccountId(accountId);
            log.info("계좌 {} (ID: {}): 기존 거래내역 {}개 삭제됨", accountName, accountId, deletedCount);

            // 4. 초기 잔액 검증 및 마이너스 잔액 보정
            List<Transaction> validatedTransactions = ensureValidInitialBalance(uniqueTransactions, account);

            // 5. 모든 거래의 잔액 재계산
            recalculateBalances(validatedTransactions);

            // 6. 거래 내역에 마이너스 잔액 있는지 확인
            boolean hasNegativeBalance = checkForNegativeBalances(validatedTransactions);

            // 마이너스 잔액이 있으면 보정 입금 추가
            if (hasNegativeBalance) {
                log.info("계좌 {} (ID: {}): 마이너스 잔액 발견, 보정 진행", accountName, accountId);
                validatedTransactions = correctNegativeBalances(validatedTransactions, account);

                // 잔액 다시 계산
                recalculateBalances(validatedTransactions);
            }

            // 7. 새 거래내역 저장
            List<Transaction> savedTransactions = new ArrayList<>();
            for (Transaction tx : validatedTransactions) {
                savedTransactions.add(transactionRepository.save(tx));
            }

            log.info("계좌 {} (ID: {}): {}개 거래내역 저장 완료",
                    accountName, accountId, savedTransactions.size());

            // 8. 계좌 잔액 업데이트 (마지막 거래의 잔액으로)
            if (!savedTransactions.isEmpty()) {
                long finalBalance = savedTransactions.get(savedTransactions.size() - 1).getUpdatedBalance();

                // 계좌 잔액과 마지막 거래 잔액이 다른 경우에만 업데이트
                if (account.getBalance() != finalBalance) {
                    log.info("계좌 {} (ID: {}): 잔액 불일치 감지 - 계좌 잔액: {}원, 거래 후 잔액: {}원",
                            accountName, accountId, account.getBalance(), finalBalance);

                    account.setBalance(finalBalance);
                    accountRepository.save(account);

                    log.info("계좌 {} (ID: {}): 잔액 동기화 완료 ({}원)",
                            accountName, accountId, finalBalance);
                } else {
                    log.info("계좌 {} (ID: {}): 잔액이 이미 일치함 ({}원)",
                            accountName, accountId, finalBalance);
                }
            }

            log.info("계좌 {} (ID: {}): 거래내역 처리 완료", accountName, accountId);
        } catch (Exception e) {
            log.error("계좌 ID {} 거래내역 처리 중 오류: {}", account.getId(), e.getMessage(), e);
            throw new RuntimeException("거래내역 처리 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 거래내역 중간에 마이너스 잔액이 있는지 확인
     */
    private boolean checkForNegativeBalances(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            if (tx.getUpdatedBalance() < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 마이너스 잔액이 발생하는 지점에 보정 입금 추가
     */
    private List<Transaction> correctNegativeBalances(List<Transaction> transactions, Account account) {
        List<Transaction> correctedTransactions = new ArrayList<>();
        long runningBalance = 0;

        for (Transaction tx : transactions) {
            long previousBalance = runningBalance;

            // 잔액 계산
            if (tx.getTransactionType() == INCOME_TYPE) {
                runningBalance += tx.getAmount();
            } else if (tx.getTransactionType() == EXPENSE_TYPE) {
                runningBalance -= tx.getAmount();
            }

            // 마이너스 잔액이 발생하면 보정 입금 추가
            if (runningBalance < 0 && previousBalance >= 0) {
                // 보정 금액 = 마이너스 금액 + 여유금
                long correctionAmount = Math.abs(runningBalance) + 500000;

                // 보정 입금 거래 생성 (같은 날짜, 1초 전)
                LocalDateTime correctionDate = tx.getTransactionDate().minusSeconds(1);

                Transaction correctionTx = Transaction.createDummyTransaction(
                        account,
                        INCOME_TYPE,
                        correctionAmount,
                        null, // 카테고리 없음
                        correctionDate,
                        "잔액 부족 보정 입금",
                        previousBalance + correctionAmount
                );

                // 보정 입금 추가
                correctedTransactions.add(correctionTx);

                // 잔액 업데이트
                runningBalance = previousBalance + correctionAmount - tx.getAmount();

                log.info("계좌 ID {}: {}에 마이너스 잔액 보정 입금 {}원 추가됨",
                        account.getId(), correctionDate, correctionAmount);
            }

            // 원래 거래 추가 (업데이트된 잔액 설정)
            tx.setUpdatedBalance(runningBalance);
            correctedTransactions.add(tx);
        }

        // 날짜순 정렬
        correctedTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

        return correctedTransactions;
    }

    /**
     * 마이너스 잔액과 불일치 감지를 위한 특수 처리
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSpecialCaseAccount(Account account) {
        try {
            // 계좌 정보
            Long accountId = account.getId();
            String accountName = account.getAccountName();

            // 블루보틀 용산점 -7,000원 같은 특수 케이스 확인
            List<Transaction> transactions = transactionRepository.findByAccountId(accountId);

            if (transactions.isEmpty()) {
                return;
            }

            // 날짜순 정렬
            transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 특수 케이스 확인: 마지막 거래 후 잔액이 마이너스인 경우
            Transaction lastTx = transactions.get(transactions.size() - 1);
            if (lastTx.getUpdatedBalance() < 0) {
                log.warn("계좌 {} (ID: {}): 마지막 거래 잔액이 마이너스 ({}원). 보정 진행",
                        accountName, accountId, lastTx.getUpdatedBalance());

                // 계좌 개설 금액이나 큰 입금 필요
                LocalDateTime now = LocalDateTime.now();
                long correctionAmount = Math.abs(lastTx.getUpdatedBalance()) + 1000000; // 마이너스 금액 + 100만원

                // 보정 입금 거래 생성 (계좌 개설 입금으로 설정)
                Transaction correctionTx = Transaction.createDummyTransaction(
                        account,
                        INCOME_TYPE,
                        correctionAmount,
                        null,
                        transactions.get(0).getTransactionDate().minusDays(1),
                        "계좌 초기 입금",
                        correctionAmount
                );

                // 거래내역 저장 및 재처리
                transactionRepository.save(correctionTx);

                // 전체 계좌 거래내역 다시 처리
                processAccountTransactions(account);

                log.info("계좌 {} (ID: {}): 마이너스 잔액 보정 완료", accountName, accountId);
            }

            // 계좌 잔액과 마지막 거래 잔액 불일치 확인
            if (account.getBalance() != lastTx.getUpdatedBalance()) {
                log.warn("계좌 {} (ID: {}): 계좌 잔액과 마지막 거래 잔액 불일치 - 계좌: {}원, 거래: {}원",
                        accountName, accountId, account.getBalance(), lastTx.getUpdatedBalance());

                // 잔액 업데이트
                account.setBalance(lastTx.getUpdatedBalance());
                accountRepository.save(account);

                log.info("계좌 {} (ID: {}): 잔액 동기화 완료 ({}원)",
                        accountName, accountId, lastTx.getUpdatedBalance());
            }
        } catch (Exception e) {
            log.error("계좌 ID {} 특수 케이스 처리 중 오류: {}", account.getId(), e.getMessage(), e);
        }
    }

    /**
     * 중복 거래내역 제거
     * - 같은 날짜(초 단위까지), 금액, 유형, 상대방이 동일한 거래는 중복으로 간주
     * - 또한 같은 날짜에 같은 금액과 유형의 거래는 추가 검증
     */
    private List<Transaction> removeDuplicateTransactions(List<Transaction> transactions) {
        Map<String, Transaction> uniqueMap = new LinkedHashMap<>();

        // 날짜별, 금액별, 유형별 거래 숫자 카운트 (심층 중복 분석용)
        Map<String, Integer> dateAmountTypeCount = new HashMap<>();

        // 1차 패스: 정확히 동일한 키를 가진 거래 처리
        for (Transaction tx : transactions) {
            // 중복 판별을 위한 고유 키 생성
            String key = createTransactionKey(tx);

            // 날짜+금액+유형 키 (중복 분석용)
            String dateAmountTypeKey = createDateAmountTypeKey(tx);
            dateAmountTypeCount.put(
                    dateAmountTypeKey,
                    dateAmountTypeCount.getOrDefault(dateAmountTypeKey, 0) + 1
            );

            // 중복이 없거나 이미 있는 거래의 ID가 더 큰 경우 대체
            // ID가 작은 것을 원본으로 간주하여 보존
            if (!uniqueMap.containsKey(key) || uniqueMap.get(key).getId() > tx.getId()) {
                uniqueMap.put(key, tx);
            }
        }

        // 2차 패스: 같은 날짜에 같은 금액과 유형의 거래가 2개 이상인 경우를 확인하여 중복 제거
        List<Transaction> result = new ArrayList<>(uniqueMap.values());

        // 2개 이상인 날짜+금액+유형 조합을 찾아 추가 필터링
        if (result.size() > 1) {
            Map<String, List<Transaction>> groupedTransactions = new HashMap<>();

            // 잠재적 중복 그룹화
            for (Transaction tx : result) {
                String dateAmountTypeKey = createDateAmountTypeKey(tx);
                if (dateAmountTypeCount.getOrDefault(dateAmountTypeKey, 0) > 1) {
                    if (!groupedTransactions.containsKey(dateAmountTypeKey)) {
                        groupedTransactions.put(dateAmountTypeKey, new ArrayList<>());
                    }
                    groupedTransactions.get(dateAmountTypeKey).add(tx);
                }
            }

            // 각 그룹에서 시간차가 30분 이내인 거래는 중복으로 처리
            List<Transaction> filteredResults = new ArrayList<>();
            Set<Long> duplicateIds = new HashSet<>();

            // 중복 ID 식별
            for (List<Transaction> group : groupedTransactions.values()) {
                if (group.size() > 1) {
                    // 날짜순 정렬
                    group.sort(Comparator.comparing(Transaction::getTransactionDate));

                    // 첫 번째 거래는 유지
                    Transaction firstTx = group.get(0);

                    // 나머지 거래는 시간차 확인하여 필터링
                    for (int i = 1; i < group.size(); i++) {
                        Transaction currentTx = group.get(i);
                        Transaction previousTx = group.get(i-1);

                        // 30분(1800초) 이내 거래는 중복으로 판단
                        long secondsDiff = Math.abs(
                                currentTx.getTransactionDate().toEpochSecond(java.time.ZoneOffset.UTC) -
                                        previousTx.getTransactionDate().toEpochSecond(java.time.ZoneOffset.UTC)
                        );

                        if (secondsDiff < 1800) {
                            // ID가 큰 쪽을 중복으로 판단
                            duplicateIds.add(Math.max(currentTx.getId(), previousTx.getId()));
                        }
                    }
                }
            }

            // 중복을 제외한 결과 생성
            for (Transaction tx : result) {
                if (!duplicateIds.contains(tx.getId())) {
                    filteredResults.add(tx);
                }
            }

            return filteredResults;
        }

        return result;
    }

    /**
     * 거래내역의 고유키 생성
     */
    private String createTransactionKey(Transaction tx) {
        // 초 단위까지만 포함한 날짜 문자열 사용 (밀리초 차이는 무시)
        String dateStr = tx.getTransactionDate().withNano(0).toString();

        return String.format("%s_%d_%d_%s",
                dateStr,
                tx.getAmount(),
                tx.getTransactionType(),
                tx.getOpponentName() == null ? "" : tx.getOpponentName());
    }

    /**
     * 날짜+금액+유형 기반 간소화된 키 생성 (중복 분석용)
     */
    private String createDateAmountTypeKey(Transaction tx) {
        // 날짜는 연월일만 사용하여 같은 날 거래 그룹화
        String dateStr = tx.getTransactionDate().toLocalDate().toString();

        return String.format("%s_%d_%d",
                dateStr,
                tx.getAmount(),
                tx.getTransactionType());
    }

    /**
     * 모든 거래의 잔액을 처음부터 재계산
     * - 계좌 개설 입금부터 시작하여 모든 거래의 잔액을 순차적으로 계산
     * - 잔액이 음수가 되는 경우 감지하여 로그 기록
     */
    private void recalculateBalances(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }

        long runningBalance = 0;
        Transaction firstTx = transactions.get(0);
        Long accountId = firstTx.getAccount().getId();

        log.info("계좌 ID {}: 거래내역 {}건의 잔액 재계산 시작", accountId, transactions.size());

        // 첫 거래가 입금이 아니면 문제 기록
        if (firstTx.getTransactionType() != INCOME_TYPE) {
            log.warn("계좌 ID {}: 첫 거래가 입금이 아닙니다. 거래유형: {}, 금액: {}원, 설명: {}",
                    accountId, firstTx.getTransactionType(), firstTx.getAmount(), firstTx.getOpponentName());
        }

        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);

            // 이전 잔액 기록 (디버깅용)
            long previousBalance = runningBalance;

            // 거래 유형에 따라 잔액 계산
            if (tx.getTransactionType() == INCOME_TYPE) {
                runningBalance += tx.getAmount();
            } else if (tx.getTransactionType() == EXPENSE_TYPE) {
                runningBalance -= tx.getAmount();
            }

            // 잔액이 음수가 되는 거래 감지
            if (runningBalance < 0 && previousBalance >= 0) {
                log.warn("계좌 ID {}: {}번째 거래에서 잔액이 음수가 됨. 이전: {}원, 현재: {}원, 거래: {} {}원, 설명: {}",
                        accountId, i+1, previousBalance, runningBalance,
                        tx.getTransactionType() == INCOME_TYPE ? "입금" : "출금",
                        tx.getAmount(), tx.getOpponentName());
            }

            // 업데이트된 잔액 설정
            tx.setUpdatedBalance(runningBalance);
        }

        log.info("계좌 ID {}: 잔액 재계산 완료. 최종 잔액: {}원", accountId, runningBalance);
    }

    /**
     * 거래내역의 유효한 초기 잔액 보장 및 중간 마이너스 잔액 처리
     * - 첫 거래가 출금이거나 중간에 잔액이 마이너스가 되는 경우 해결
     * - 마이너스 잔액이 발생하는 시점에 보정 입금 추가
     */
    private List<Transaction> ensureValidInitialBalance(List<Transaction> transactions, Account account) {
        if (transactions.isEmpty()) {
            return transactions;
        }

        // 결과 거래 목록 (초기 입금 및 보정 입금이 추가될 수 있음)
        List<Transaction> resultTransactions = new ArrayList<>();

        // 첫 거래가 출금인 경우 초기 입금 추가
        if (transactions.get(0).getTransactionType() == EXPENSE_TYPE) {
            long firstWithdrawal = transactions.get(0).getAmount();
            // 초기 입금액 (출금액 + 여유금 50만원)
            long initialAmount = firstWithdrawal + 500000;

            // 첫 거래보다 1일 전 날짜로 초기 입금 생성
            LocalDateTime initialDate = transactions.get(0).getTransactionDate().minusDays(1);

            // 초기 입금 트랜잭션 생성
            Transaction initialDeposit = Transaction.createDummyTransaction(
                    account,
                    INCOME_TYPE,
                    initialAmount,
                    null, // 카테고리 없음
                    initialDate,
                    "계좌 개설 입금",
                    initialAmount
            );

            // 초기 입금 추가
            resultTransactions.add(initialDeposit);
            log.info("계좌 ID {}: 출금으로 시작하는 거래 보정. 초기 입금 {}원 추가됨", account.getId(), initialAmount);
        }

        // 모든 거래 순회하며 중간에 마이너스 잔액이 되는 지점 확인 및 보정
        long runningBalance = 0;

        // 결과 목록에 이미 초기 입금이 추가된 경우 그 금액을 시작 잔액으로 설정
        if (!resultTransactions.isEmpty()) {
            runningBalance = resultTransactions.get(0).getAmount();
        }

        for (Transaction tx : transactions) {
            // 거래 전 잔액 확인
            long previousBalance = runningBalance;

            // 거래 유형에 따라 잔액 변동
            if (tx.getTransactionType() == INCOME_TYPE) {
                runningBalance += tx.getAmount();
            } else if (tx.getTransactionType() == EXPENSE_TYPE) {
                runningBalance -= tx.getAmount();
            }

            // 마이너스 잔액이 발생하는 경우 보정 입금 추가
            if (runningBalance < 0 && previousBalance >= 0) {
                // 마이너스 금액 + 여유금 10만원
                long correctionAmount = Math.abs(runningBalance) + 100000;

                // 현재 거래와 동일한 시간에 입금 생성 (0.1초 전)
                LocalDateTime correctionDate = tx.getTransactionDate().minusNanos(100000000);

                // 보정 입금 생성
                Transaction correctionDeposit = Transaction.createDummyTransaction(
                        account,
                        INCOME_TYPE,
                        correctionAmount,
                        null, // 카테고리 없음
                        correctionDate,
                        "잔액 부족 보정",
                        previousBalance + correctionAmount
                );

                // 보정 입금 추가
                resultTransactions.add(correctionDeposit);

                // 잔액 업데이트
                runningBalance += correctionAmount;

                log.info("계좌 ID {}: {}에 마이너스 잔액 보정 입금 {}원 추가됨",
                        account.getId(), tx.getTransactionDate(), correctionAmount);
            }

            // 원래 거래 추가
            resultTransactions.add(tx);
        }

        // 날짜순 정렬
        resultTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

        // 보정 거래가 추가되었는지 확인
        if (resultTransactions.size() > transactions.size()) {
            log.info("계좌 ID {}: {}개의 보정 거래가 추가됨",
                    account.getId(), resultTransactions.size() - transactions.size());
        }

        return resultTransactions;
    }

    /**
     * 특정 회원의 모든 계좌 거래내역 검증 및 수정
     */
    @Transactional
    public void processAllAccountsForMember(Long memberId) {
        List<Account> memberAccounts = accountRepository.findByMemberId(memberId);
        log.info("회원 ID {}: {} 개 계좌의 거래내역 처리 시작", memberId, memberAccounts.size());

        for (Account account : memberAccounts) {
            try {
                processAccountTransactions(account);
            } catch (Exception e) {
                log.error("회원 ID {} 계좌 ID {} 처리 중 오류: {}",
                        memberId, account.getId(), e.getMessage(), e);
            }
        }

        log.info("회원 ID {}: 모든 계좌 거래내역 처리 완료", memberId);
    }
}