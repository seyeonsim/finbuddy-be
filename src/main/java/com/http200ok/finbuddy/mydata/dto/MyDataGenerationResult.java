package com.http200ok.finbuddy.mydata.dto;

import lombok.*;

/**
 * 회원별 데이터 생성 결과를 담는 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyDataGenerationResult {
    private Long memberId;
    private String memberName;
    private int checkingAccountsCreated;
    private int depositAccountsCreated;
    private int savingAccountsCreated;
    private int transactionsCreated;
    private boolean success;
    private String message;

    // 생성 메서드
    public static MyDataGenerationResult createResult(
            Long memberId, String memberName, int checkingAccountsCreated,
            int depositAccountsCreated, int savingAccountsCreated,
            int transactionsCreated, boolean success, String message) {

        MyDataGenerationResult result = new MyDataGenerationResult();
        result.setMemberId(memberId);
        result.setMemberName(memberName);
        result.setCheckingAccountsCreated(checkingAccountsCreated);
        result.setDepositAccountsCreated(depositAccountsCreated);
        result.setSavingAccountsCreated(savingAccountsCreated);
        result.setTransactionsCreated(transactionsCreated);
        result.setSuccess(success);
        result.setMessage(message);
        return result;
    }
}