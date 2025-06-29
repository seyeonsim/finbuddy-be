package com.http200ok.finbuddy.transfer.controller;

import com.http200ok.finbuddy.security.CustomUserDetails;
import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.transfer.dto.AutoTransferCreateRequestDto;
import com.http200ok.finbuddy.transfer.dto.AutoTransferResponseDto;
import com.http200ok.finbuddy.transfer.dto.AutoTransferUpdateRequestDto;
import com.http200ok.finbuddy.transfer.service.AutoTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/autotransfer")
public class AutoTransferController {

    private final AutoTransferService autoTransferService;

    // 자동이체 생성
    @PostMapping
    public ResponseEntity<AutoTransferResponseDto> createAutoTransfer(
            @RequestBody AutoTransferCreateRequestDto request) {

        AutoTransfer autoTransfer = autoTransferService.createAutoTransfers(
                request.getFromAccountId(),
                request.getTargetBankName(),
                request.getTargetAccountNumber(),
                request.getAmount(),
                request.getTransferDay()
        );

        return ResponseEntity.ok(new AutoTransferResponseDto(autoTransfer));
    }

    // 특정 회원의 자동이체 목록 조회 API
    @GetMapping("/list")
    public ResponseEntity<List<AutoTransferResponseDto>> getAutoTransfersByMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<AutoTransferResponseDto> responseList = autoTransferService.getAutoTransfersByMember(memberId)
                .stream()
                .map(AutoTransferResponseDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/{autoTransferId}")
    public ResponseEntity<AutoTransferResponseDto> getAutoTransferById(@PathVariable("autoTransferId") Long autoTransferId) {
        AutoTransfer autoTransfer = autoTransferService.getAutoTransferById(autoTransferId);
        return ResponseEntity.ok(new AutoTransferResponseDto(autoTransfer));
    }

    // 자동이체 정보 수정(금액, 날짜)
    @PatchMapping("/{autoTransferId}")
    public ResponseEntity<Void> updateAutoTransfer(
            @PathVariable("autoTransferId") Long autoTransferId,
            @RequestBody AutoTransferUpdateRequestDto requestDto) {
        autoTransferService.updateAutoTransfer(autoTransferId, requestDto);
        return ResponseEntity.noContent().build();
    }

    // 자동이체 상태 변경(활성/비활성화 토글)
    @PatchMapping("/{autoTransferId}/toggle-status")
    public ResponseEntity<Void> toggleAutoTransferStatus(@PathVariable("autoTransferId") Long autoTransferId) {
        autoTransferService.toggleAutoTransferStatus(autoTransferId);
        return ResponseEntity.noContent().build();
    }

    // 자동이체 정보 삭제
    @DeleteMapping("/{autoTransferId}")
    public ResponseEntity<Void> deleteAutoTransfer(@PathVariable("autoTransferId") Long autoTransferId) {
        autoTransferService.deleteAutoTransfer(autoTransferId);
        return ResponseEntity.noContent().build();
    }
}
