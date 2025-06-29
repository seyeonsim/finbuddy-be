package com.http200ok.finbuddy.mydata.controller;

import com.http200ok.finbuddy.mydata.dto.MyDataDeletionResult;
import com.http200ok.finbuddy.mydata.dto.MyDataGenerationResult;
import com.http200ok.finbuddy.mydata.service.MyDataService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * MyData 더미 데이터 생성 및 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
public class MyDataController {

    private final MyDataService myDataService;

    /**
     * 특정 회원의 MyData 더미 데이터를 생성합니다.
     * @return 데이터 생성 결과
     */
    @PostMapping("/generate")
    public ResponseEntity<MyDataGenerationResult> generateDataForMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        MyDataGenerationResult result = myDataService.generateDummyDataForMember(memberId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 특정 회원의 기존 MyData 더미 데이터를 삭제합니다.
     *
     * @param memberId 데이터를 삭제할 회원 ID
     * @return 데이터 삭제 결과
     */
    @DeleteMapping("/delete/{memberId}")
    public ResponseEntity<MyDataDeletionResult> deleteDataForMember(@PathVariable("memberId") Long memberId) {
        MyDataDeletionResult result = myDataService.deleteExistingDataForMember(memberId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

}