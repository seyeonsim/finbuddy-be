package com.http200ok.finbuddy.transfer.dto;

import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import lombok.Getter;

@Getter
public class AutoTransferResponseDto {

    private final Long id;
    private final String fromAccountNumber;
    private final String targetBankName;
    private final String targetAccountNumber;
    private final Long amount;
    private final Integer transferDay;
    private final String status;

    public AutoTransferResponseDto(AutoTransfer autoTransfer) {
        this.id = autoTransfer.getId();
        this.fromAccountNumber = autoTransfer.getAccount().getAccountNumber();
        this.targetBankName = autoTransfer.getTargetBankName();
        this.targetAccountNumber = autoTransfer.getTargetAccountNumber();
        this.amount = autoTransfer.getAmount();
        this.transferDay = autoTransfer.getTransferDay();
        this.status = autoTransfer.getStatus().name();
    }
}
