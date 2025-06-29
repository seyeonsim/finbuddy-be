package com.http200ok.finbuddy.transfer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequestDto {
    private Long fromAccountId;        // 출금 계좌 ID
    private String toBankName;         // 입금 은행명
    private String toAccountNumber;    // 입금 계좌번호
    private Long amount;
    private String password;
    private String senderName;
    private String receiverName;
}
