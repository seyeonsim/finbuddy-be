package com.http200ok.finbuddy.transfer.domain;

public enum AutoTransferStatus {
    ACTIVE, // 자동이체 진행중
    INACTIVE, // 비활성화됨 (일시정지)
    FAILED // 자동이체 실패
}
