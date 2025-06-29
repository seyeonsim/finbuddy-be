package com.http200ok.finbuddy.transfer.domain;

import com.http200ok.finbuddy.account.domain.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auto_transfer_id")
    private Long id;

    // 출금 계좌
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 입금 은행명
    @Column(nullable = false)
    private String targetBankName;

    // 입금 계좌번호
    @Column(nullable = false)
    private String targetAccountNumber;

    @Column(nullable = false)
    private Long amount;

    // 매월 몇 일에 실행할지
    @Column(nullable = false)
    private Integer transferDay;

    // 자동이체 상태 (ACTIVE, INACTIVE, FAILED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AutoTransferStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성 메서드
    public static AutoTransfer createAutoTransfer(Account account, String targetBankName, String targetAccountNumber, Long amount, Integer transferDay) {
        AutoTransfer autoTransfer = new AutoTransfer();
        autoTransfer.account = account;
        autoTransfer.targetBankName = targetBankName;
        autoTransfer.targetAccountNumber = targetAccountNumber;
        autoTransfer.amount = amount;
        autoTransfer.transferDay = transferDay;
        autoTransfer.status = AutoTransferStatus.ACTIVE;
        autoTransfer.createdAt = LocalDateTime.now();
        autoTransfer.updatedAt = LocalDateTime.now();
        return autoTransfer;
    }

    // 자동이체 정보 수정 메서드
    public void updateTransferInfo(Long amount, Integer transferDay) {
        this.amount = amount;
        this.transferDay = transferDay;
        this.status = AutoTransferStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // Status 변경 메서드
    public void toggleActiveStatus() {
        if (this.status == AutoTransferStatus.ACTIVE) {
            this.status = AutoTransferStatus.INACTIVE;
        } else if (this.status == AutoTransferStatus.INACTIVE) {
            this.status = AutoTransferStatus.ACTIVE;
        } else if (this.status == AutoTransferStatus.FAILED) {
            this.status = AutoTransferStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = AutoTransferStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsActive() {
        this.status = AutoTransferStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}
