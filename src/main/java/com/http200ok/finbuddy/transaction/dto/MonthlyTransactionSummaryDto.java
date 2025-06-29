package com.http200ok.finbuddy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyTransactionSummaryDto {
    private Long depositTotal;   // 입금 합계
    private Long withdrawalTotal; // 출금 합계
}
