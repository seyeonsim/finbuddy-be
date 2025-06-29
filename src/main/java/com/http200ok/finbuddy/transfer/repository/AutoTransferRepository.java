package com.http200ok.finbuddy.transfer.repository;

import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.transfer.domain.AutoTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AutoTransferRepository extends JpaRepository<AutoTransfer, Long> {
    List<AutoTransfer> findByStatus(AutoTransferStatus status);

    // 특정 회원의 자동이체 목록 조회 (account.member.id 기준)
    @Query("SELECT at FROM AutoTransfer at JOIN FETCH at.account WHERE at.account.member.id = :memberId")
    List<AutoTransfer> findByAccount_Member_Id(@Param("memberId") Long memberId);

    // 자동이체 날짜와 활성/비활성 상태
    List<AutoTransfer> findByTransferDayAndStatus(Integer transferDay, AutoTransferStatus status);

    @Query("SELECT a FROM AutoTransfer a " +
            "WHERE a.transferDay IN :targetDays " +
            "AND a.status = 'ACTIVE'")
    List<AutoTransfer> findForScheduledExecution(@Param("targetDays") List<Integer> targetDays);

    @Query("SELECT a FROM AutoTransfer a WHERE a.transferDay > :dayOfMonth AND a.status = 'ACTIVE'")
    List<AutoTransfer> findTransfersAfterDay(@Param("dayOfMonth") int dayOfMonth);
}
