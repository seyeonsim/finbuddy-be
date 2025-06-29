package com.http200ok.finbuddy.mydata.dto;

import lombok.*;

/**
 * 데이터 삭제 결과를 담는 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyDataDeletionResult {
    private Long memberId;
    private String memberName;
    private int accountsDeleted;
    private int transactionsDeleted;
    private boolean success;
    private String message;

    // 생성 메서드
    public static MyDataDeletionResult createResult(
            Long memberId, String memberName, int accountsDeleted,
            int transactionsDeleted, boolean success, String message) {

        MyDataDeletionResult result = new MyDataDeletionResult();
        result.setMemberId(memberId);
        result.setMemberName(memberName);
        result.setAccountsDeleted(accountsDeleted);
        result.setTransactionsDeleted(transactionsDeleted);
        result.setSuccess(success);
        result.setMessage(message);
        return result;
    }
}