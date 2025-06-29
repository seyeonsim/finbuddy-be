package com.http200ok.finbuddy.mydata.service;

import com.http200ok.finbuddy.mydata.dto.MyDataDeletionResult;
import com.http200ok.finbuddy.mydata.dto.MyDataGenerationResult;

/**
 * MyData 더미 데이터 생성을 위한 서비스 인터페이스
 */
public interface MyDataService {

    /**
     * 특정 회원에 대한 더미 데이터를 생성합니다.
     *
     * @param memberId 대상 회원 ID
     * @return 생성된 계좌 및 거래내역 개수 정보
     */
    MyDataGenerationResult generateDummyDataForMember(Long memberId);

    /**
     * 특정 회원의 기존 더미 데이터를 삭제합니다.
     *
     * @param memberId 대상 회원 ID
     * @return 삭제된 계좌 및 거래내역 개수
     */
    MyDataDeletionResult deleteExistingDataForMember(Long memberId);
}