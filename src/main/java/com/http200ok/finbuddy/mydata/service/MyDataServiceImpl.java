package com.http200ok.finbuddy.mydata.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.domain.AccountType;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.bank.repository.BankRepository;
import com.http200ok.finbuddy.category.repository.CategoryRepository;
import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import com.http200ok.finbuddy.mydata.dto.MyDataDeletionResult;
import com.http200ok.finbuddy.mydata.dto.MyDataGenerationResult;
import com.http200ok.finbuddy.product.domain.*;
import com.http200ok.finbuddy.product.repository.CheckingProductRepository;
import com.http200ok.finbuddy.product.repository.DepositProductRepository;
import com.http200ok.finbuddy.product.repository.SavingProductRepository;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import com.http200ok.finbuddy.transaction.repository.TransactionRepository;
import com.http200ok.finbuddy.transaction.service.TransactionFixService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyDataServiceImpl implements MyDataService {

    // 레포지토리 의존성
    private final MemberRepository memberRepository;
    private final BankRepository bankRepository;
    private final AccountRepository accountRepository;
    private final DepositProductRepository depositProductRepository;
    private final SavingProductRepository savingProductRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CheckingProductRepository checkingProductRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final PasswordEncoder passwordEncoder;

    // 가게 이름 목록
    private final Map<Long, List<String>> categoryStores = new HashMap<>();

    // 지출/수입 거래 유형 (1: 입금, 2: 출금)
    private static final int INCOME_TYPE = 1;
    private static final int EXPENSE_TYPE = 2;

    // 계좌 최소 잔액 설정
    private static final long MINIMUM_MAIN_BALANCE = 500000; // 메인 계좌 최소 잔액 50만원

    @Override
    @Transactional
    public MyDataGenerationResult generateDummyDataForMember(Long memberId) {
        try {
            // 카테고리별 가게 정보 초기화
            initCategoryStores();

            // 회원 찾기
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberId));

            System.out.println("회원 " + member.getName() + "(ID: " + member.getId() + ")에 대한 더미 데이터 생성 시작");

            // 1. 계좌 생성
            List<Account> checkingAccounts = generateCheckingAccounts(member);
            Account mainAccount = checkingAccounts.getFirst();
            List<Account> depositAccounts = generateDepositAccounts(member);
            List<Account> savingAccounts = generateSavingAccounts(member);

            // 2. 메인 계좌 거래내역 생성
            System.out.println("메인 계좌 거래내역 생성 시작");
            int transactionCount = generateTransactionsForMainAccount(mainAccount);
            System.out.println("메인 계좌 거래내역 " + transactionCount + "개 생성 완료");

            // 3. 계좌 간 거래내역 생성
            System.out.println("계좌 간 거래내역 생성 시작");
            int interAccountTransactions = generateInterAccountTransactions(mainAccount, checkingAccounts, depositAccounts, savingAccounts);
            transactionCount += interAccountTransactions;
            System.out.println("계좌 간 거래내역 " + interAccountTransactions + "개 생성 완료");

            // 4. 모든 계좌의 거래내역 검증 및 수정
            System.out.println("모든 거래내역 검증 및 수정 시작");
            validateAndFixAllTransactions(checkingAccounts, depositAccounts, savingAccounts);

            // 5. 모든 계좌의 최종 잔액 확인
            List<Account> allAccounts = new ArrayList<>();
            allAccounts.addAll(checkingAccounts);
            allAccounts.addAll(depositAccounts);
            allAccounts.addAll(savingAccounts);

            // 각 계좌 유형별 최종 목록 새로 조회
            List<Account> finalCheckingAccounts = new ArrayList<>();
            for (Account account : checkingAccounts) {
                Account refreshed = accountRepository.findById(account.getId()).orElse(account);
                finalCheckingAccounts.add(refreshed);
            }

            List<Account> finalDepositAccounts = new ArrayList<>();
            for (Account account : depositAccounts) {
                Account refreshed = accountRepository.findById(account.getId()).orElse(account);
                finalDepositAccounts.add(refreshed);
            }

            List<Account> finalSavingAccounts = new ArrayList<>();
            for (Account account : savingAccounts) {
                Account refreshed = accountRepository.findById(account.getId()).orElse(account);
                finalSavingAccounts.add(refreshed);
            }

            System.out.println("회원 " + member.getName() + "(ID: " + member.getId() + ")의 더미 데이터 생성 완료");
            System.out.println("입출금 계좌: " + finalCheckingAccounts.size() + "개");
            System.out.println("예금 계좌: " + finalDepositAccounts.size() + "개");
            System.out.println("적금 계좌: " + finalSavingAccounts.size() + "개");
            System.out.println("총 거래: " + transactionCount + "개");

            // 결과 반환
            return MyDataGenerationResult.createResult(
                    member.getId(),
                    member.getName(),
                    finalCheckingAccounts.size(),
                    finalDepositAccounts.size(),
                    finalSavingAccounts.size(),
                    transactionCount,
                    true,
                    "데이터 생성 완료"
            );

        } catch (Exception e) {
            System.err.println("회원 ID " + memberId + "에 대한 더미 데이터 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return MyDataGenerationResult.createResult(
                    memberId,
                    null,
                    0,
                    0,
                    0,
                    0,
                    false,
                    "오류 발생: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public MyDataDeletionResult deleteExistingDataForMember(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberId));

            // 회원의 기존 계좌 찾기
            List<Account> existingAccounts = accountRepository.findByMemberId(memberId);
            int accountCount = existingAccounts.size();
            AtomicInteger transactionCount = new AtomicInteger();

            // 계좌별 거래내역 삭제 - 별도 트랜잭션으로 처리
            for (Account account : existingAccounts) {
                final Long accountId = account.getId();

                // 트랜잭션 템플릿으로 별도 트랜잭션에서 실행
                transactionTemplate.execute(status -> {
                    try {
                        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
                        transactionCount.addAndGet(transactions.size());
                        transactionRepository.deleteByAccountId(accountId);
                        return null;
                    } catch (Exception e) {
                        System.err.println("계좌 ID " + accountId + "의 거래내역 삭제 중 오류: " + e.getMessage());
                        status.setRollbackOnly();
                        return null;
                    }
                });
            }

            // 계좌 삭제
            accountRepository.deleteByMemberId(memberId);

            System.out.println("회원 " + member.getName() + "(ID: " + memberId + ")의 기존 데이터 삭제 완료. 계좌 "
                    + accountCount + "개, 거래내역 " + transactionCount + "개 삭제됨");

            return MyDataDeletionResult.createResult(
                    memberId,
                    member.getName(),
                    accountCount,
                    transactionCount.get(),
                    true,
                    "데이터 삭제 완료"
            );

        } catch (Exception e) {
            System.err.println("회원 ID " + memberId + "의 기존 데이터 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return MyDataDeletionResult.createResult(
                    memberId,
                    null,
                    0,
                    0,
                    false,
                    "오류 발생: " + e.getMessage()
            );
        }
    }

    /**
     * 모든 계좌의 거래내역을 검증하고 수정하는 메서드
     * - 마이너스 잔액 확인 및 자동 수정
     * - 계좌 잔액과 마지막 거래 잔액 동기화
     */
    private void validateAndFixAllTransactions(List<Account> checkingAccounts,
                                               List<Account> depositAccounts,
                                               List<Account> savingAccounts) {
        try {
            System.out.println("===== 거래내역 최종 검증 및 수정 시작 =====");

            // 모든 계좌 목록 합치기
            List<Account> allAccounts = new ArrayList<>();
            allAccounts.addAll(checkingAccounts);
            allAccounts.addAll(depositAccounts);
            allAccounts.addAll(savingAccounts);

            System.out.println("총 " + allAccounts.size() + "개 계좌 처리 시작");

            // 1. 각 계좌별로 거래내역 확인 및 검증
            for (Account account : allAccounts) {
                try {
                    // 현재 영속성 컨텍스트 초기화
                    entityManager.flush();
                    entityManager.clear();

                    // 계좌 다시 로드
                    Account refreshedAccount = accountRepository.findById(account.getId())
                            .orElseThrow(() -> new RuntimeException("계좌를 찾을 수 없습니다: " + account.getId()));

                    System.out.println("계좌 ID " + refreshedAccount.getId() + " (" + refreshedAccount.getAccountName() +
                            ") 처리: 현재 잔액 " + refreshedAccount.getBalance() + "원");

                    // 거래내역 조회
                    List<Transaction> transactions = transactionRepository.findByAccountId(refreshedAccount.getId());
                    if (transactions.isEmpty()) {
                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 거래내역 없음");
                        continue;
                    }

                    // 날짜순 정렬
                    transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

                    // 마이너스 잔액 확인 및 수정
                    boolean hasNegativeBalance = false;
                    long negativeBalancePoint = 0;
                    LocalDateTime firstNegativeDate = null;

                    // 잔액 재계산
                    long runningBalance = 0;
                    for (Transaction tx : transactions) {
                        if (tx.getTransactionType() == INCOME_TYPE) {
                            runningBalance += tx.getAmount();
                        } else {
                            runningBalance -= tx.getAmount();
                        }

                        // 마이너스 잔액 발견
                        if (runningBalance < 0 && !hasNegativeBalance) {
                            hasNegativeBalance = true;
                            negativeBalancePoint = runningBalance;
                            firstNegativeDate = tx.getTransactionDate();
                            System.out.println("계좌 ID " + refreshedAccount.getId() + ": 마이너스 잔액 발견 (" +
                                    runningBalance + "원), 날짜: " + firstNegativeDate);
                        }
                    }

                    // 마이너스 잔액이 있는 경우 보정 입금 추가
                    if (hasNegativeBalance) {
                        // 마이너스가 되기 직전 거래 바로 뒤에 보정 입금 생성
                        long correctionAmount = Math.abs(negativeBalancePoint) + 5000000; // 500만원 여유
                        LocalDateTime correctionDate = firstNegativeDate.minusMinutes(5); // 5분 전

                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 마이너스 잔액 보정 입금 생성 - " +
                                correctionAmount + "원, 날짜: " + correctionDate);

                        // 보정 입금 거래 생성
                        Transaction correctionTx = Transaction.createDummyTransaction(
                                refreshedAccount,
                                INCOME_TYPE,
                                correctionAmount,
                                categoryRepository.findById(7L).orElse(null), // 기타 카테고리
                                correctionDate,
                                "마이너스 잔액 방지 입금",
                                correctionAmount // 입금 시점 잔액
                        );

                        transactionRepository.save(correctionTx);

                        // 거래내역 재조회하여 모든 거래 재처리
                        transactions = transactionRepository.findByAccountId(refreshedAccount.getId());
                        transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

                        // 잔액 다시 계산
                        runningBalance = 0;
                        for (Transaction tx : transactions) {
                            if (tx.getTransactionType() == INCOME_TYPE) {
                                runningBalance += tx.getAmount();
                            } else {
                                runningBalance -= tx.getAmount();
                            }

                            // 업데이트된 잔액 설정
                            if (tx.getUpdatedBalance() != runningBalance) {
                                tx.setUpdatedBalance(runningBalance);
                                transactionRepository.save(tx);
                            }
                        }

                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 잔액 재계산 완료, 최종 잔액: " + runningBalance + "원");
                    } else {
                        // 마이너스 잔액이 없어도 거래내역 잔액 확인 및 수정
                        boolean needsUpdate = false;
                        runningBalance = 0;

                        for (Transaction tx : transactions) {
                            if (tx.getTransactionType() == INCOME_TYPE) {
                                runningBalance += tx.getAmount();
                            } else {
                                runningBalance -= tx.getAmount();
                            }

                            // 기존 잔액과 다른 경우 업데이트
                            if (tx.getUpdatedBalance() != runningBalance) {
                                tx.setUpdatedBalance(runningBalance);
                                transactionRepository.save(tx);
                                needsUpdate = true;
                            }
                        }

                        if (needsUpdate) {
                            System.out.println("계좌 ID " + refreshedAccount.getId() + ": 일부 거래 잔액 오류 수정됨");
                        }
                    }

                    // 계좌 잔액 업데이트 (마지막 거래의 잔액으로)
                    Transaction lastTransaction = transactions.get(transactions.size() - 1);
                    long lastBalance = lastTransaction.getUpdatedBalance();

                    if (refreshedAccount.getBalance() != lastBalance) {
                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 계좌 잔액 불일치 감지 - " +
                                "계좌 잔액: " + refreshedAccount.getBalance() + "원, " +
                                "마지막 거래 잔액: " + lastBalance + "원");

                        refreshedAccount.setBalance(lastBalance);
                        accountRepository.save(refreshedAccount);

                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 계좌 잔액 " + lastBalance + "원으로 업데이트");
                    }

                    System.out.println("계좌 ID " + refreshedAccount.getId() + " 처리 완료: 최종 잔액 " + lastBalance + "원");
                } catch (Exception e) {
                    System.err.println("계좌 ID " + account.getId() + " 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 2. 최종 검증 - 마이너스 잔액이 있는지 다시 확인
            System.out.println("모든 계좌 최종 검증 시작");

            for (Account account : allAccounts) {
                try {
                    Account refreshedAccount = accountRepository.findById(account.getId()).orElse(account);

                    if (refreshedAccount.getBalance() < 0) {
                        System.err.println("경고: 계좌 ID " + refreshedAccount.getId() +
                                " (" + refreshedAccount.getAccountName() + ")의 최종 잔액이 여전히 마이너스입니다: " +
                                refreshedAccount.getBalance() + "원");

                        // 긴급 보정 - 강제로 양수 잔액으로 변경
                        long correctionAmount = Math.abs(refreshedAccount.getBalance()) + 1000000; // 100만원 여유
                        refreshedAccount.setBalance(correctionAmount);
                        accountRepository.save(refreshedAccount);

                        LocalDateTime now = LocalDateTime.now();

                        Transaction emergencyTx = Transaction.createDummyTransaction(
                                refreshedAccount,
                                INCOME_TYPE,
                                correctionAmount,
                                categoryRepository.findById(7L).orElse(null),
                                now,
                                "긴급 잔액 보정",
                                correctionAmount
                        );

                        transactionRepository.save(emergencyTx);

                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 긴급 잔액 보정 완료 - 최종 잔액: " +
                                correctionAmount + "원");
                    }
                } catch (Exception e) {
                    System.err.println("계좌 ID " + account.getId() + " 최종 검증 중 오류: " + e.getMessage());
                }
            }

            System.out.println("===== 모든 계좌의 거래내역 검증 및 수정 완료 =====");
        } catch (Exception e) {
            System.err.println("거래내역 검증 및 수정 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initCategoryStores() {
        // 식비 (카테고리 ID: 1)
        categoryStores.put(1L, Arrays.asList(
                "맥도날드", "버거킹", "롯데리아", "KFC", "아웃백", "VIPS", "서브웨이",
                "김밥천국", "본죽", "교촌치킨", "BBQ", "BHC", "피자헛", "도미노피자",
                "명인만두", "본죽&비빔밥", "이디야 카페", "CU", "GS25", "세븐일레븐"
        ));

        // 쇼핑 (카테고리 ID: 2)
        categoryStores.put(2L, Arrays.asList(
                "유니클로", "자라", "H&M", "나이키", "아디다스", "무신사", "올리브영",
                "롯데백화점", "신세계백화점", "현대백화점", "이마트", "홈플러스", "코스트코",
                "쿠팡", "G마켓", "11번가", "SSG닷컴", "티몬", "위메프", "ABC마트"
        ));

        // 오락 (카테고리 ID: 3)
        categoryStores.put(3L, Arrays.asList(
                "CGV", "메가박스", "롯데시네마", "넷플릭스", "왓챠", "디즈니플러스",
                "티빙", "스포티파이", "멜론", "지니뮤직", "플로", "Xbox Game Pass",
                "PlayStation Store", "스팀", "닌텐도", "NEXON", "NC SOFT", "VR ZONE"
        ));

        // 카페 (카테고리 ID: 4)
        categoryStores.put(4L, Arrays.asList(
                "스타벅스", "투썸플레이스", "이디야", "폴바셋", "커피빈", "할리스",
                "파스쿠찌", "엔제리너스", "매머드커피", "빽다방", "컴포즈커피", "더벤티",
                "메가커피", "달콤커피", "공차", "요거프레소", "블루보틀", "폴인커피"
        ));

        // 교통 (카테고리 ID: 5)
        categoryStores.put(5L, Arrays.asList(
                "서울교통공사", "카카오T", "타다", "우버", "쏘카", "그린카",
                "한국철도공사", "SRT", "티머니", "캐시비", "레일플러스", "코레일"
        ));

        // 주거/통신 (카테고리 ID: 6)
        categoryStores.put(6L, Arrays.asList(
                "한국전력공사", "서울도시가스", "경동도시가스", "가스공사", "한국수자원공사",
                "SKT", "KT", "LG U+", "SK브로드밴드", "LG헬로비전", "월세", "관리비"
        ));

        // 기타 (카테고리 ID: 7)
        categoryStores.put(7L, Arrays.asList(
                "이체", "ATM 출금", "보험료", "택배비", "구독료", "세금", "적금", "예금",
                "의료비", "교육비", "경조사비", "기부금", "자산", "용돈", "급여"
        ));
    }

    private List<Account> generateCheckingAccounts(Member member) {
        List<Account> accounts = new ArrayList<>();
        List<Bank> banks = bankRepository.findAll();
        List<CheckingProduct> checkingProducts = checkingProductRepository.findAll();

        // 랜덤하게 은행 선택 (중복 없이)
        List<Bank> selectedBanks = getRandomElements(banks, 5);

        // 메인 계좌 먼저 생성 (잔액이 더 많고 거래가 많은 계좌)
        Bank mainBank = selectedBanks.getFirst();
        CheckingProduct mainProduct = getRandomCheckingProductForBank(checkingProducts, mainBank.getId());

        Account mainAccount = Account.createAccount(
                member,
                mainBank,
                mainProduct,
                null,
                mainProduct.getName(),
                generateRandomDigits(11),
                passwordEncoder.encode("1234"),
                AccountType.CHECKING,
                randomAmount(5000, 10000) * 1000, // 메인 계좌는 더 많은 초기 잔액
                randomDateWithinLastYear(),
                null
        );

        accounts.add(accountRepository.save(mainAccount));

        // 나머지 입출금 계좌 생성
        for (int i = 1; i < 5; i++) {
            Bank bank = selectedBanks.get(i);
            CheckingProduct product = getRandomCheckingProductForBank(checkingProducts, bank.getId());

            Account account = Account.createAccount(
                    member,
                    bank,
                    product,
                    null,
                    product.getName(),
                    generateRandomDigits(11),
                    passwordEncoder.encode("1234"),
                    AccountType.CHECKING,
                    randomAmount(100, 800) * 1000, // 1000원 단위
                    randomDateWithinLastYear(),
                    null
            );

            accounts.add(accountRepository.save(account));
        }

        return accounts;
    }

    private List<Account> generateDepositAccounts(Member member) {
        List<Account> accounts = new ArrayList<>();
        List<Bank> banks = bankRepository.findAll();
        List<DepositProduct> depositProducts = depositProductRepository.findAll();

        // 랜덤하게 은행 선택 (중복 없이)
        List<Bank> selectedBanks = getRandomElements(banks, 2);

        for (int i = 0; i < 2; i++) {
            Bank bank = selectedBanks.get(i);
            DepositProduct product = getRandomDepositProductForBank(depositProducts, bank.getId());

            if (product != null && !product.getOptions().isEmpty()) {
                // 상품 옵션 중 하나를 랜덤하게 선택
                DepositProductOption option = getRandomElement(product.getOptions());

                // 생성 날짜와 만기 날짜 설정
                LocalDateTime createdAt = randomDateWithinLastYear();
                LocalDateTime maturedAt;

                // 만기 날짜는 생성 날짜로부터 option의 saving_term 개월 후
                if (option != null && option.getSavingTerm() != null) {
                    maturedAt = createdAt.plusMonths(option.getSavingTerm());
                } else {
                    maturedAt = createdAt.plusYears(1); // 기본값 1년
                }

                // 예금 계좌의 잔액 설정 (500만원~2000만원) - 만원 단위로 조정
                long balance = (randomAmount(500, 2000) * 10000);

                Account account = Account.createAccount(
                        member,
                        bank,
                        product,
                        option,
                        product.getName(), // 상품 이름 사용
                        generateRandomDigits(12),
                        passwordEncoder.encode("1234"),
                        AccountType.DEPOSIT,
                        balance,
                        createdAt,
                        maturedAt
                );

                accounts.add(accountRepository.save(account));
            }
        }

        return accounts;
    }

    private List<Account> generateSavingAccounts(Member member) {
        List<Account> accounts = new ArrayList<>();
        List<Bank> banks = bankRepository.findAll();
        List<SavingProduct> savingProducts = savingProductRepository.findAll();

        // 랜덤하게 은행 선택 (중복 없이)
        List<Bank> selectedBanks = getRandomElements(banks, 2);

        for (int i = 0; i < 2; i++) {
            Bank bank = selectedBanks.get(i);
            SavingProduct product = getRandomSavingProductForBank(savingProducts, bank.getId());

            if (product != null && !product.getOptions().isEmpty()) {
                // 상품 옵션 중 하나를 랜덤하게 선택
                SavingProductOption option = getRandomElement(product.getOptions());

                // 생성 날짜와 만기 날짜 설정
                LocalDateTime createdAt = randomDateWithinLastYear();
                LocalDateTime maturedAt;

                // 만기 날짜는 생성 날짜로부터 option의 saving_term 개월 후
                if (option != null && option.getSavingTerm() != null) {
                    maturedAt = createdAt.plusMonths(option.getSavingTerm());
                } else {
                    maturedAt = createdAt.plusYears(1); // 기본값 1년
                }

                // 적금 계좌의 잔액 설정 (50만원~500만원) - 만원 단위로 조정
                long balance = (randomAmount(50, 500) * 10000);

                Account account = Account.createAccount(
                        member,
                        bank,
                        product,
                        option,
                        product.getName(), // 상품 이름 사용
                        generateRandomDigits(13),
                        passwordEncoder.encode("1234"),
                        AccountType.SAVING,
                        balance,
                        createdAt,
                        maturedAt
                );

                accounts.add(accountRepository.save(account));
            }
        }

        return accounts;
    }

    /**
     * 메인 계좌의 거래내역 생성 시 마이너스 잔액 방지
     */
    private int generateTransactionsForMainAccount(Account mainAccount) {
        try {
            List<Transaction> transactions = new ArrayList<>();

            // 1. 계좌 생성일 확인
            LocalDateTime accountCreationDate = mainAccount.getCreatedAt();
            if (accountCreationDate == null) {
                accountCreationDate = LocalDateTime.now().minusMonths(12); // 기본값: 1년 전
            }

            System.out.println("메인 계좌 ID " + mainAccount.getId() + ": 거래내역 생성 시작 (생성일: " + accountCreationDate + ")");

            // 2. 계좌 개설 입금 거래 생성 (첫 거래) - 충분히 큰 금액으로 시작
            long initialDeposit = randomAmount(50000, 80000) * 1000; // 2천만~3천만원으로 증액
            System.out.println("메인 계좌 초기 입금: " + initialDeposit + "원");

            Transaction initialTx = Transaction.createDummyTransaction(
                    mainAccount,
                    INCOME_TYPE,
                    initialDeposit,
                    categoryRepository.findById(7L).orElse(null), // 기타 카테고리
                    accountCreationDate,
                    "계좌 개설 입금",
                    initialDeposit
            );

            transactions.add(initialTx);
            long currentBalance = initialDeposit;

            // 3. 급여 입금 거래 생성 (매월 25일)
            LocalDateTime salaryStartDate = accountCreationDate.plusMonths(1).withDayOfMonth(25);
            LocalDateTime now = LocalDateTime.now();

            while (salaryStartDate.isBefore(now)) {
                // 급여 증액: 1000원 단위, 400만~600만원
                long salaryAmount = randomAmount(4000, 6000) * 1000;
                currentBalance += salaryAmount;

                Transaction salaryTx = Transaction.createDummyTransaction(
                        mainAccount,
                        INCOME_TYPE,
                        salaryAmount,
                        categoryRepository.findById(7L).orElse(null), // 기타 카테고리
                        salaryStartDate,
                        getRandomCompanyName() + " 급여",
                        currentBalance
                );

                transactions.add(salaryTx);
                salaryStartDate = salaryStartDate.plusMonths(1); // 다음 달로 이동
            }

            // 4. 기타 대규모 입금 추가 (여러 번)
            for (int i = 0; i < 3; i++) {
                LocalDateTime largeIncomeDate = randomDateBetween(accountCreationDate, now);
                // 큰 금액 입금 (1천만원~2천만원)
                long largeIncomeAmount = randomAmount(10000, 20000) * 1000;
                currentBalance += largeIncomeAmount;

                String[] incomeTypes = {"투자 수익", "부동산 매각", "상여금", "주식 매도", "퇴직금", "보험금"};

                Transaction largeIncomeTx = Transaction.createDummyTransaction(
                        mainAccount,
                        INCOME_TYPE,
                        largeIncomeAmount,
                        categoryRepository.findById(7L).orElse(null), // 기타 카테고리
                        largeIncomeDate,
                        incomeTypes[randomInt(0, incomeTypes.length - 1)],
                        currentBalance
                );

                transactions.add(largeIncomeTx);
                System.out.println("대규모 입금 추가: " + largeIncomeAmount + "원, 잔액: " + currentBalance + "원");
            }

            // 5. 기타 일반 수입 거래 (용돈, 환급금 등)
            for (int i = 0; i < randomInt(5, 10); i++) {
                LocalDateTime incomeDate = randomDateBetween(accountCreationDate, now);
                // 1000원 단위로 설정
                long incomeAmount = randomAmount(100, 1000) * 1000;
                currentBalance += incomeAmount;

                Transaction incomeTx = Transaction.createDummyTransaction(
                        mainAccount,
                        INCOME_TYPE,
                        incomeAmount,
                        categoryRepository.findById(7L).orElse(null), // 기타 카테고리
                        incomeDate,
                        getRandomPersonName() + " 송금",
                        currentBalance
                );

                transactions.add(incomeTx);
            }

            // 6. 지출 거래 - 각 카테고리별로 생성
            Map<LocalDate, Set<String>> dailyStores = new HashMap<>(); // 일별 가게 방문 추적용

            for (long categoryId = 1; categoryId <= 6; categoryId++) { // 기타 카테고리(7)는 제외
                int transactionCount = randomInt(8, 20); // 거래 수 증가

                for (int i = 0; i < transactionCount; i++) {
                    LocalDateTime expenseDate = randomDateBetween(accountCreationDate, now);
                    LocalDate expenseDay = expenseDate.toLocalDate();

                    // 카테고리별 금액 설정 - 1000원 단위로 조정
                    long expenseAmount = (getRandomAmountForCategory(categoryId) / 1000) * 1000;
//
//                    // 큰 금액 지출 추가 (특별 경우)
//                    if (Math.random() < 0.2) { // 20% 확률로 큰 금액 지출
//                        expenseAmount *= randomInt(5, 20); // 5~20배 큰 금액
//                    }

                    // 해당 날짜에 이미 방문한 가게 목록 가져오기
                    Set<String> visitedStores = dailyStores.getOrDefault(expenseDay, new HashSet<>());

                    // 랜덤 가게 선택
                    String store = getRandomStoreForCategory(categoryId);

                    // 이미 같은 날 방문한 가게인지 확인
                    if (visitedStores.contains(store)) {
                        // 이미 방문한 가게면 50% 확률로 스킵
                        if (Math.random() < 0.5) {
                            continue;
                        }
                    }

                    // 중요: 잔액이 충분한지 확인
                    if (currentBalance - expenseAmount < 500000) { // 최소 50만원 유지
                        // 잔액 부족 시, 추가 입금 진행
                        long additionalDeposit = expenseAmount + 2000000; // 지출액 + 200만원 (여유분)
                        currentBalance += additionalDeposit;

                        Transaction additionalDepositTx = Transaction.createDummyTransaction(
                                mainAccount,
                                INCOME_TYPE,
                                additionalDeposit,
                                categoryRepository.findById(7L).orElse(null),
                                expenseDate.minusHours(2), // 지출 2시간 전에 입금
                                "지출 대비 자금 보충",
                                currentBalance
                        );

                        transactions.add(additionalDepositTx);
                        System.out.println("자금 부족으로 추가 입금: " + additionalDeposit + "원, 잔액: " + currentBalance + "원");
                    }

                    // 지출 처리
                    currentBalance -= expenseAmount;

                    // 해당 일자에 방문한 가게 목록에 추가
                    visitedStores.add(store);
                    dailyStores.put(expenseDay, visitedStores);

                    Transaction expenseTx = Transaction.createDummyTransaction(
                            mainAccount,
                            EXPENSE_TYPE,
                            expenseAmount,
                            categoryRepository.findById(categoryId).orElse(null),
                            expenseDate,
                            store,
                            currentBalance
                    );

                    transactions.add(expenseTx);
                }
            }

            // 7. 대규모 지출 몇 건 추가 (차량 구매, 가전제품 등)
            for (int i = 0; i < randomInt(2, 5); i++) {
                LocalDateTime largeExpenseDate = randomDateBetween(accountCreationDate, now);

                // 큰 금액 지출 (500만원~3천만원)
                long largeExpenseAmount = randomAmount(5000, 30000) * 1000;

                // 잔액 확인 및 필요시 추가 입금
                if (currentBalance - largeExpenseAmount < 1000000) { // 최소 100만원 유지
                    long additionalDeposit = largeExpenseAmount + 5000000; // 지출액 + 500만원 (여유분)
                    currentBalance += additionalDeposit;

                    Transaction additionalDepositTx = Transaction.createDummyTransaction(
                            mainAccount,
                            INCOME_TYPE,
                            additionalDeposit,
                            categoryRepository.findById(7L).orElse(null),
                            largeExpenseDate.minusDays(1), // 지출 하루 전에 입금
                            "대규모 지출 대비 자금 준비",
                            currentBalance
                    );

                    transactions.add(additionalDepositTx);
                    System.out.println("대규모 지출 대비 추가 입금: " + additionalDeposit + "원, 잔액: " + currentBalance + "원");
                }

                // 지출 처리
                currentBalance -= largeExpenseAmount;

                String[] largeExpenseTypes = {"가전제품 구매", "가구 구매", "차량 구매", "보험료 납부", "전세 보증금", "주택 수리비", "여행 경비"};

                Transaction largeExpenseTx = Transaction.createDummyTransaction(
                        mainAccount,
                        EXPENSE_TYPE,
                        largeExpenseAmount,
                        categoryRepository.findById(2L).orElse(null), // 쇼핑 카테고리
                        largeExpenseDate,
                        largeExpenseTypes[randomInt(0, largeExpenseTypes.length - 1)],
                        currentBalance
                );

                transactions.add(largeExpenseTx);
                System.out.println("대규모 지출: " + largeExpenseAmount + "원, 잔액: " + currentBalance + "원");
            }

            // 8. 거래 날짜 기준 정렬
            transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 9. 중복 제거 및 잔액 재계산 (가장 오래된 거래부터)
            Map<String, Transaction> uniqueTransactions = new HashMap<>();
            long runningBalance = 0;

            for (Transaction tx : transactions) {
                // 거래 유형에 따른 잔액 계산
                if (tx.getTransactionType() == INCOME_TYPE) {
                    runningBalance += tx.getAmount();
                } else {
                    runningBalance -= tx.getAmount();
                }

                // 업데이트된 잔액 설정
                tx.setUpdatedBalance(runningBalance);

                // 중복 거래 확인 키 생성
                String key = tx.getTransactionDate().toLocalDate() + "_" +
                        tx.getAmount() + "_" +
                        tx.getTransactionType() + "_" +
                        tx.getOpponentName();

                // 중복이 없거나 ID가 더 작은 경우 저장
                if (!uniqueTransactions.containsKey(key) ||
                        (tx.getId() != null && uniqueTransactions.get(key).getId() != null &&
                                tx.getId() < uniqueTransactions.get(key).getId())) {
                    uniqueTransactions.put(key, tx);
                }
            }

            // 10. 최종 거래 목록 및 저장
            List<Transaction> finalTransactions = new ArrayList<>(uniqueTransactions.values());
            finalTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 잔액 다시 계산
            runningBalance = 0;
            for (Transaction tx : finalTransactions) {
                if (tx.getTransactionType() == INCOME_TYPE) {
                    runningBalance += tx.getAmount();
                } else {
                    runningBalance -= tx.getAmount();
                }
                tx.setUpdatedBalance(runningBalance);
            }

            // 최종 저장
            for (Transaction tx : finalTransactions) {
                transactionRepository.save(tx);
            }

            // 최종 잔액 업데이트
            mainAccount.setBalance(runningBalance);
            accountRepository.save(mainAccount);

            System.out.println("메인 계좌 ID " + mainAccount.getId() + ": " + finalTransactions.size() +
                    "개 거래내역 생성 완료. 최종 잔액: " + runningBalance + "원");

            return finalTransactions.size();
        } catch (Exception e) {
            System.err.println("메인 계좌 거래내역 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 계좌 간 거래내역 생성 시 마이너스 잔액 방지
     */
    private int generateInterAccountTransactions(Account mainAccount, List<Account> checkingAccounts,
                                                 List<Account> depositAccounts, List<Account> savingAccounts) {
        int totalTransactions = 0;

        try {
            System.out.println("계좌 간 거래내역 생성 시작");

            // 1. 모든 계좌 목록 합치기
            List<Account> allAccounts = new ArrayList<>();
            allAccounts.addAll(checkingAccounts);
            allAccounts.addAll(depositAccounts);
            allAccounts.addAll(savingAccounts);

            // 2. 다른 입출금 계좌와의 이체
            System.out.println("입출금 계좌 간 이체 거래 생성");
            for (int i = 1; i < checkingAccounts.size(); i++) {
                Account targetAccount = checkingAccounts.get(i);

                // 각 계좌에 대해 2~4회 이체
                for (int j = 0; j < randomInt(2, 4); j++) {
                    // 이체 금액 (3~10만원)
                    long transferAmount = randomAmount(30, 100) * 1000;
                    LocalDateTime transferDate = randomDateWithinLastMonth();

                    // 메인 계좌 최신 상태 조회
                    mainAccount = accountRepository.findById(mainAccount.getId()).orElse(mainAccount);
                    long mainAccountBalance = mainAccount.getBalance();

                    // 최소 잔액 유지 확인 - 잔액 부족시 추가 입금
                    if (mainAccountBalance - transferAmount < MINIMUM_MAIN_BALANCE) {
                        // 잔액 부족 시 추가 입금 처리
                        long additionalAmount = transferAmount + MINIMUM_MAIN_BALANCE - mainAccountBalance + 1000000; // 여유분 추가
                        LocalDateTime additionalDate = transferDate.minusHours(1); // 이체 1시간 전

                        // 추가 입금 거래 생성
                        Transaction additionalTx = Transaction.createDummyTransaction(
                                mainAccount,
                                INCOME_TYPE,
                                additionalAmount,
                                categoryRepository.findById(7L).orElse(null),
                                additionalDate,
                                "이체 준비금",
                                mainAccountBalance + additionalAmount
                        );
                        transactionRepository.save(additionalTx);
                        totalTransactions++;

                        // 메인 계좌 잔액 업데이트
                        mainAccountBalance += additionalAmount;
                        mainAccount.setBalance(mainAccountBalance);
                        accountRepository.save(mainAccount);

                        System.out.println("이체 준비금 입금: " + additionalAmount + "원, 잔액: " + mainAccountBalance + "원");
                    }

                    // 이체 실행 - 메인 계좌에서 차감
                    mainAccountBalance -= transferAmount;

                    // 출금 거래 생성 - 상대방 이름으로 계좌 주인 표시
                    Transaction outTx = Transaction.createDummyTransaction(
                            mainAccount,
                            EXPENSE_TYPE,
                            transferAmount,
                            categoryRepository.findById(7L).orElse(null), // 기타(이체)
                            transferDate,
                            targetAccount.getMember().getName(), // 계좌 주인 이름
                            mainAccountBalance // 출금 후 잔액
                    );
                    transactionRepository.save(outTx);
                    totalTransactions++;

                    // 메인 계좌 잔액 업데이트
                    mainAccount.setBalance(mainAccountBalance);
                    accountRepository.save(mainAccount);

                    // 대상 계좌 최신 상태 조회 및 입금
                    targetAccount = accountRepository.findById(targetAccount.getId()).orElse(targetAccount);
                    long targetAccountBalance = targetAccount.getBalance() + transferAmount;

                    // 입금 거래 생성 - 상대방 이름으로 계좌 주인 표시
                    Transaction inTx = Transaction.createDummyTransaction(
                            targetAccount,
                            INCOME_TYPE,
                            transferAmount,
                            categoryRepository.findById(7L).orElse(null), // 기타(이체)
                            transferDate,
                            mainAccount.getMember().getName(), // 계좌 주인 이름
                            targetAccountBalance // 입금 후 잔액
                    );
                    transactionRepository.save(inTx);
                    totalTransactions++;

                    // 대상 계좌 잔액 업데이트
                    targetAccount.setBalance(targetAccountBalance);
                    accountRepository.save(targetAccount);

                    System.out.println("이체 완료: " + mainAccount.getMember().getName() + " -> " +
                            targetAccount.getMember().getName() + ", 금액: " + transferAmount + "원");
                }
            }

            // 3. 예금 계좌 거래
            System.out.println("예금 계좌 거래 생성");
            for (Account depositAccount : depositAccounts) {
                // 예금 금액은 계좌 잔액으로 설정
                long depositAmount = depositAccount.getBalance();
                LocalDateTime depositDate = depositAccount.getCreatedAt();

                // 메인 계좌 최신 상태 조회
                mainAccount = accountRepository.findById(mainAccount.getId()).orElse(mainAccount);
                long mainAccountBalance = mainAccount.getBalance();

                // 메인 계좌에 충분한 잔액이 없으면 추가 입금
                if (mainAccountBalance < depositAmount + MINIMUM_MAIN_BALANCE) {
                    long additionalIncome = depositAmount + MINIMUM_MAIN_BALANCE - mainAccountBalance + 1000000; // 여유분 100만원
                    LocalDateTime additionalIncomeDate = depositDate.minusDays(1);

                    // 준비금 입금
                    Transaction additionalIncomeTx = Transaction.createDummyTransaction(
                            mainAccount,
                            INCOME_TYPE,
                            additionalIncome,
                            categoryRepository.findById(7L).orElse(null),
                            additionalIncomeDate,
                            "예금 준비금",
                            mainAccountBalance + additionalIncome
                    );
                    transactionRepository.save(additionalIncomeTx);
                    totalTransactions++;

                    // 메인 계좌 잔액 업데이트
                    mainAccountBalance += additionalIncome;
                    mainAccount.setBalance(mainAccountBalance);
                    accountRepository.save(mainAccount);

                    System.out.println("예금 준비금 입금: " + additionalIncome + "원, 메인 계좌 잔액: " + mainAccountBalance + "원");
                }

                // 예금 가입 - 메인 계좌에서 출금
                mainAccountBalance -= depositAmount;

                Transaction outTx = Transaction.createDummyTransaction(
                        mainAccount,
                        EXPENSE_TYPE,
                        depositAmount,
                        categoryRepository.findById(7L).orElse(null),
                        depositDate,
                        depositAccount.getAccountName() + " 가입",
                        mainAccountBalance
                );
                transactionRepository.save(outTx);
                totalTransactions++;

                // 메인 계좌 잔액 업데이트
                mainAccount.setBalance(mainAccountBalance);
                accountRepository.save(mainAccount);

                // 예금 계좌 입금
                long depositAccountBalance = depositAmount;

                Transaction inTx = Transaction.createDummyTransaction(
                        depositAccount,
                        INCOME_TYPE,
                        depositAmount,
                        categoryRepository.findById(7L).orElse(null),
                        depositDate,
                        "신규 예금 가입",
                        depositAccountBalance
                );
                transactionRepository.save(inTx);
                totalTransactions++;

                // 예금 계좌 잔액 업데이트
                depositAccount.setBalance(depositAccountBalance);
                accountRepository.save(depositAccount);

                System.out.println("예금 가입: " + depositAmount + "원, 계좌잔액: " + depositAccountBalance + "원");

                // 이자 지급 거래 추가
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime interestDate = depositDate.plusMonths(3);
                int interestCount = 0;

                while (interestDate.isBefore(now) && interestCount < 3) {
                    // 이자율: 연 2~5%
                    double annualRate = 2.0 + (Math.random() * 3.0);
                    double monthlyRate = annualRate / 12.0 / 100.0;
                    long interestAmount = (long)(depositAccountBalance * monthlyRate * 3); // 3개월 이자
                    interestAmount = Math.max(interestAmount, 10000); // 최소 1만원

                    depositAccountBalance += interestAmount;

                    Transaction interestTx = Transaction.createDummyTransaction(
                            depositAccount,
                            INCOME_TYPE,
                            interestAmount,
                            categoryRepository.findById(7L).orElse(null),
                            interestDate,
                            "분기 이자 지급",
                            depositAccountBalance // 이자 추가 후 잔액
                    );
                    transactionRepository.save(interestTx);
                    totalTransactions++;

                    // 예금 계좌 잔액 업데이트
                    depositAccount.setBalance(depositAccountBalance);
                    accountRepository.save(depositAccount);

                    System.out.println("예금 이자 지급: " + interestAmount + "원, 총잔액: " + depositAccountBalance + "원");

                    interestDate = interestDate.plusMonths(3);
                    interestCount++;
                }
            }

            // 4. 적금 계좌 거래
            System.out.println("적금 계좌 거래 생성");
            for (Account savingAccount : savingAccounts) {
                LocalDateTime startDate = savingAccount.getCreatedAt();
                LocalDateTime now = LocalDateTime.now();

                // 적금 총액을 월별로 나누어 계산
                long monthsBetween = startDate.toLocalDate().until(now.toLocalDate()).toTotalMonths();
                if (monthsBetween <= 0) monthsBetween = 1;

                long savingTotal = savingAccount.getBalance();
                long monthlyDeposit = (savingTotal / monthsBetween) / 10000 * 10000; // 만원 단위
                if (monthlyDeposit < 10000) monthlyDeposit = 10000; // 최소 1만원

                // 마지막 납입액 조정
                long totalDeposited = monthlyDeposit * (monthsBetween - 1);
                long lastDeposit = savingTotal - totalDeposited;
                if (lastDeposit < 0) {
                    lastDeposit = monthlyDeposit;
                    monthsBetween = (savingTotal / monthlyDeposit) + 1;
                }

                // 적금 잔액 진행 트래킹
                long savingBalance = 0;

                // 각 월별 납입 처리
                for (int i = 0; i < monthsBetween; i++) {
                    LocalDateTime depositDate = startDate.plusMonths(i);
                    if (depositDate.isAfter(now)) break; // 미래 날짜는 처리하지 않음

                    // 납입액 결정
                    long depositAmount = (i == monthsBetween - 1) ? lastDeposit : monthlyDeposit;

                    // 메인 계좌 최신 상태 조회
                    mainAccount = accountRepository.findById(mainAccount.getId()).orElse(mainAccount);
                    long mainAccountBalance = mainAccount.getBalance();

                    // 메인 계좌에 충분한 잔액이 없으면 추가 입금
                    if (mainAccountBalance < depositAmount + MINIMUM_MAIN_BALANCE) {
                        long additionalIncome = depositAmount + MINIMUM_MAIN_BALANCE - mainAccountBalance + 500000; // 여유분
                        LocalDateTime additionalIncomeDate = depositDate.minusDays(1);

                        // 준비금 입금
                        Transaction additionalIncomeTx = Transaction.createDummyTransaction(
                                mainAccount,
                                INCOME_TYPE,
                                additionalIncome,
                                categoryRepository.findById(7L).orElse(null),
                                additionalIncomeDate,
                                "적금 납입 준비금",
                                mainAccountBalance + additionalIncome
                        );
                        transactionRepository.save(additionalIncomeTx);
                        totalTransactions++;

                        // 메인 계좌 잔액 업데이트
                        mainAccountBalance += additionalIncome;
                        mainAccount.setBalance(mainAccountBalance);
                        accountRepository.save(mainAccount);

                        System.out.println("적금 준비금 입금: " + additionalIncome + "원, 메인 계좌 잔액: " + mainAccountBalance + "원");
                    }

                    // 적금 납입 - 메인 계좌에서 출금
                    mainAccountBalance -= depositAmount;

                    Transaction outTx = Transaction.createDummyTransaction(
                            mainAccount,
                            EXPENSE_TYPE,
                            depositAmount,
                            categoryRepository.findById(7L).orElse(null),
                            depositDate,
                            savingAccount.getAccountName() + " " + (i+1) + "회차 납입",
                            mainAccountBalance
                    );
                    transactionRepository.save(outTx);
                    totalTransactions++;

                    // 메인 계좌 잔액 업데이트
                    mainAccount.setBalance(mainAccountBalance);
                    accountRepository.save(mainAccount);

                    // 적금 계좌 입금
                    savingBalance += depositAmount;

                    Transaction inTx = Transaction.createDummyTransaction(
                            savingAccount,
                            INCOME_TYPE,
                            depositAmount,
                            categoryRepository.findById(7L).orElse(null),
                            depositDate,
                            (i+1) + "회차 납입",
                            savingBalance
                    );
                    transactionRepository.save(inTx);
                    totalTransactions++;

                    // 적금 계좌 잔액 업데이트
                    savingAccount.setBalance(savingBalance);
                    accountRepository.save(savingAccount);

                    System.out.println("적금 납입: " + depositAmount + "원, 총잔액: " + savingBalance + "원 (" + (i+1) + "회차)");
                }
            }

            // 5. 모든 계좌의 최종 잔액 확인 및 업데이트
            System.out.println("모든 계좌의 최종 잔액 확인 및 업데이트");
            for (Account account : allAccounts) {
                try {
                    // 거래내역 조회
                    List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
                    if (transactions.isEmpty()) continue;

                    // 날짜순 정렬
                    transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

                    // 마지막 거래 잔액으로 계좌 잔액 설정
                    Transaction lastTransaction = transactions.get(transactions.size() - 1);
                    long lastBalance = lastTransaction.getUpdatedBalance();

                    // 계좌 잔액 업데이트
                    if (account.getBalance() != lastBalance) {
                        account.setBalance(lastBalance);
                        accountRepository.save(account);

                        System.out.println("계좌 ID " + account.getId() + " (" + account.getAccountName() +
                                "): 잔액 업데이트 " + account.getBalance() + "원 -> " + lastBalance + "원");
                    }
                } catch (Exception e) {
                    System.err.println("계좌 ID " + account.getId() + " 최종 잔액 업데이트 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 6. 마이너스 잔액 최종 검증
            System.out.println("마이너스 잔액 최종 검증");
            for (Account account : allAccounts) {
                try {
                    Account refreshedAccount = accountRepository.findById(account.getId()).orElse(account);

                    if (refreshedAccount.getBalance() < 0) {
                        System.err.println("경고: 계좌 ID " + refreshedAccount.getId() + " (" +
                                refreshedAccount.getAccountName() + ") 잔액이 마이너스입니다: " +
                                refreshedAccount.getBalance() + "원");

                        // 마이너스 잔액 보정
                        long correctionAmount = Math.abs(refreshedAccount.getBalance()) + 1000000; // 100만원 여유
                        LocalDateTime now = LocalDateTime.now();

                        // 보정 입금 거래 생성
                        Transaction correctionTx = Transaction.createDummyTransaction(
                                refreshedAccount,
                                INCOME_TYPE,
                                correctionAmount,
                                categoryRepository.findById(7L).orElse(null),
                                now,
                                "마이너스 잔액 보정 입금",
                                correctionAmount
                        );

                        transactionRepository.save(correctionTx);
                        totalTransactions++;

                        // 계좌 잔액 업데이트
                        refreshedAccount.setBalance(correctionAmount);
                        accountRepository.save(refreshedAccount);

                        System.out.println("계좌 ID " + refreshedAccount.getId() + ": 마이너스 잔액 보정 완료. " +
                                "새 잔액: " + correctionAmount + "원");
                    }
                } catch (Exception e) {
                    System.err.println("계좌 ID " + account.getId() + " 마이너스 잔액 검증 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("계좌 간 거래내역 생성 완료: 총 " + totalTransactions + "개");
            return totalTransactions;
        } catch (Exception e) {
            System.err.println("계좌 간 거래내역 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            return totalTransactions;
        }
    }

    /**
     * 거래 후 계좌 잔액 정확하게 업데이트하는 메서드
     * - 모든 거래가 끝난 후 마지막 거래 잔액을 계좌 잔액으로 설정
     */
    private void updateFinalAccountBalances(List<Account> accounts) {
        System.out.println("모든 계좌의 최종 잔액 업데이트 시작");

        for (Account account : accounts) {
            try {
                // 현재 영속성 컨텍스트 초기화
                entityManager.flush();
                entityManager.clear();

                // 계좌 다시 로드
                Account refreshedAccount = accountRepository.findById(account.getId())
                        .orElseThrow(() -> new RuntimeException("계좌를 찾을 수 없습니다: " + account.getId()));

                // 거래내역 조회 (날짜순 정렬)
                List<Transaction> transactions = transactionRepository.findByAccountId(refreshedAccount.getId());
                if (transactions.isEmpty()) {
                    System.out.println("계좌 ID " + refreshedAccount.getId() + ": 거래내역 없음");
                    continue;
                }

                transactions.sort(Comparator.comparing(Transaction::getTransactionDate));

                // 마지막 거래 잔액 확인
                Transaction lastTransaction = transactions.get(transactions.size() - 1);
                long lastBalance = lastTransaction.getUpdatedBalance();

                // 계좌 잔액과 마지막 거래 잔액 비교
                if (refreshedAccount.getBalance() != lastBalance) {
                    System.out.println("계좌 ID " + refreshedAccount.getId() +
                            " (" + refreshedAccount.getAccountName() + "): 잔액 업데이트 " +
                            refreshedAccount.getBalance() + "원 -> " + lastBalance + "원");

                    // 계좌 잔액 업데이트
                    refreshedAccount.setBalance(lastBalance);
                    accountRepository.save(refreshedAccount);
                } else {
                    System.out.println("계좌 ID " + refreshedAccount.getId() +
                            " (" + refreshedAccount.getAccountName() + "): 잔액 일치 확인 (" + lastBalance + "원)");
                }
            } catch (Exception e) {
                System.err.println("계좌 ID " + account.getId() + " 최종 잔액 업데이트 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("모든 계좌의 최종 잔액 업데이트 완료");
    }

    // 유틸리티 메서드들
    private <T> List<T> getRandomElements(List<T> list, int count) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.stream().limit(count).collect(Collectors.toList());
    }

    private <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int index = (int) (Math.random() * list.size());
        return list.get(index);
    }

    private DepositProduct getRandomDepositProductForBank(List<DepositProduct> products, Long bankId) {
        List<DepositProduct> bankProducts = products.stream()
                .filter(p -> p.getBank() != null && p.getBank().getId().equals(bankId))
                .collect(Collectors.toList());

        if (bankProducts.isEmpty()) {
            return null;
        }

        return getRandomElement(bankProducts);
    }

    private SavingProduct getRandomSavingProductForBank(List<SavingProduct> products, Long bankId) {
        List<SavingProduct> bankProducts = products.stream()
                .filter(p -> p.getBank() != null && p.getBank().getId().equals(bankId))
                .collect(Collectors.toList());

        if (bankProducts.isEmpty()) {
            return null;
        }

        return getRandomElement(bankProducts);
    }

    private CheckingProduct getRandomCheckingProductForBank(List<CheckingProduct> products, Long bankId) {
        List<CheckingProduct> bankProducts = products.stream()
                .filter(p -> p.getBank() != null && p.getBank().getId().equals(bankId))
                .collect(Collectors.toList());

        if (bankProducts.isEmpty()) {
            return null;
        }

        return getRandomElement(bankProducts);
    }

    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    private long randomAmount(long min, long max) {
        return min + (long) (Math.random() * (max - min + 1));
    }

    private int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    private LocalDateTime randomDateWithinLastYear() {
        LocalDateTime now = LocalDateTime.now();
        long daysToSubtract = (long) (Math.random() * 365) + 1;
        return now.minusDays(daysToSubtract);
    }

    private LocalDateTime randomDateWithinLastMonth() {
        LocalDateTime now = LocalDateTime.now();
        long daysToSubtract = (long) (Math.random() * 30) + 1;
        return now.minusDays(daysToSubtract);
    }

    private LocalDateTime randomDateBetween(LocalDateTime start, LocalDateTime end) {
        long startEpochDay = start.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay();
        long endEpochDay = end.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay();

        if (startEpochDay >= endEpochDay) {
            return start.plusHours(randomInt(1, 10));
        }

        long daysBetween = endEpochDay - startEpochDay;
        long randomDays = (long) (Math.random() * daysBetween);

        LocalDate date = LocalDate.ofEpochDay(startEpochDay + randomDays);
        int hour = (int) (Math.random() * 14) + 8; // 8시~22시 사이
        int minute = (int) (Math.random() * 60);

        return LocalDateTime.of(date, java.time.LocalTime.of(hour, minute));
    }

    private String getRandomCompanyName() {
        String[] companies = {
                "(주)삼성전자", "(주)현대자동차", "(주)LG전자", "(주)SK텔레콤", "(주)네이버",
                "(주)카카오", "(주)쿠팡", "(주)우아한형제들", "(주)당근마켓", "(주)토스",
                "(주)라인", "(주)배달의민족", "(주)야놀자", "(주)직방", "(주)마이리얼트립"
        };
        return companies[(int) (Math.random() * companies.length)];
    }

    private String getRandomPersonName() {
        String[] firstNames = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "전", "홍"};
        String[] lastNames = {"민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "준서", "준우",
                "서연", "서윤", "지우", "서현", "민서", "하은", "하윤", "윤서", "지민", "채원"};

        return firstNames[(int) (Math.random() * firstNames.length)] + lastNames[(int) (Math.random() * lastNames.length)];
    }

    private String getRandomStoreForCategory(Long categoryId) {
        List<String> stores = categoryStores.getOrDefault(categoryId, new ArrayList<>());
        if (stores.isEmpty()) {
            return "일반 가맹점";
        }

        String store = stores.get((int) (Math.random() * stores.size()));

        // 지점명 추가 (50% 확률)
        if ((int) (Math.random() * 2) == 1) {
            String[] locations = {"강남점", "홍대점", "명동점", "신촌점", "종로점", "여의도점", "잠실점", "용산점", "건대점", "영등포점"};
            store += " " + locations[(int) (Math.random() * locations.length)];
        }

        return store;
    }

    private long getRandomAmountForCategory(Long categoryId) {
        return switch (categoryId.intValue()) {
            case 1 -> // 식비
                    randomAmount(5000, 50000);
            case 2 -> // 쇼핑
                    randomAmount(10000, 200000);
            case 3 -> // 오락
                    randomAmount(10000, 100000);
            case 4 -> // 카페
                    randomAmount(4000, 15000);
            case 5 -> // 교통
                    randomAmount(1250, 30000);
            case 6 -> // 주거/통신
                    randomAmount(50000, 500000);
            case 7 -> // 기타
                    randomAmount(10000, 200000);
            default -> randomAmount(1000, 50000);
        };
    }
}