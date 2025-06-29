package com.http200ok.finbuddy.transfer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AutoTransferUpdateRequestDto {
    private Long amount;      // 변경할 금액
    private Integer transferDay; // 변경할 날짜 (매월 몇 일)
}