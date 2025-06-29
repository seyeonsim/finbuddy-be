package com.http200ok.finbuddy.transfer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AutoTransferCreateRequestDto {
        private Long fromAccountId;          // 사용자가 선택한 출금 계좌 ID
        private String targetBankName;       // 입금 은행
        private String targetAccountNumber;  // 입금 계좌 번호
        private Long amount;                 // 자동이체 금액
        private Integer transferDay;         // 매월 몇 일에 이체할지
}
