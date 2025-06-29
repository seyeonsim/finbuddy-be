package com.http200ok.finbuddy.account.domain;

import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.bank.domain.Bank;
import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.product.domain.Product;
import com.http200ok.finbuddy.product.domain.ProductOption;
import com.http200ok.finbuddy.transaction.domain.Transaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    /**
     * CHECKING(보통예금), DEPOSIT(예금), SAVING(적금)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private Long balance;

    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime maturedAt;

    /**
     * 1) Account - Transaction (1:N)
     * 2) cascade: Account가 삭제되면 Transaction도 삭제
     * 3) orphanRemoval: Account가 가진 트랜잭션을 List에서 제거하면 DB에서도 제거
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutoTransfer> autoTransfers = new ArrayList<>();

    // 상품 참조 (보통예금은 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // 선택된 상품 옵션 (보통예금 또는 옵션 선택 안한 경우 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption selectedOption;

    // 생성 메서드
    public static Account createAccount(
            Member member, Bank bank, Product product, ProductOption productOption,
            String accountName, String accountNumber, String password,
            AccountType accountType, Long balance, LocalDateTime createdAt, LocalDateTime maturedAt) {

        Account account = new Account();
        account.setMember(member);
        account.setBank(bank);
        account.setProduct(product);
        account.setSelectedOption(productOption);
        account.setAccountName(accountName);
        account.setAccountNumber(accountNumber);
        account.setPassword(password);
        account.setAccountType(accountType);
        account.setBalance(balance);
        account.setCreatedAt(createdAt);
        account.setMaturedAt(maturedAt);
        return account;
    }
}
