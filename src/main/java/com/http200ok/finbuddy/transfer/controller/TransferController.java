package com.http200ok.finbuddy.transfer.controller;

import com.http200ok.finbuddy.account.dto.CheckingAccountResponseDto;
import com.http200ok.finbuddy.account.dto.ReceivingAccountResponseDto;
import com.http200ok.finbuddy.budget.service.BudgetService;
import com.http200ok.finbuddy.security.CustomUserDetails;
import com.http200ok.finbuddy.transfer.dto.TransferRequestDto;
import com.http200ok.finbuddy.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final BudgetService budgetService;

    // 이체 시 입출금 계좌 조회
    @GetMapping("/all/checking-account")
    public ResponseEntity<List<CheckingAccountResponseDto>> getCheckingAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<CheckingAccountResponseDto> checkingAccountList = transferService.getCheckingAccountList(memberId);
        return ResponseEntity.ok(checkingAccountList);
    }

    // 이체 시 입금 계좌 조회
    @GetMapping("/receiving-account")
    public ResponseEntity<ReceivingAccountResponseDto> getReceivingAccount(@RequestParam("bankName") String bankName, @RequestParam("accountNumber") String accountNumber) {
        System.out.println("bankName = " + bankName);
        ReceivingAccountResponseDto receivingAccount = transferService.getReceivingAccount(bankName, accountNumber);
        return ResponseEntity.ok(receivingAccount);
    }

    // 계좌 이체 API
    @PostMapping
    public ResponseEntity<?> executeTransfer(@RequestBody TransferRequestDto transferRequestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();

        boolean result = transferService.executeAccountTransfer(
                memberId,
                transferRequestDto.getFromAccountId(),
                transferRequestDto.getToBankName(),
                transferRequestDto.getToAccountNumber(),
                transferRequestDto.getAmount(),
                transferRequestDto.getPassword(),
                transferRequestDto.getSenderName(),
                transferRequestDto.getReceiverName()
        );

        // 이체 성공 후 예산 초과 확인 및 알림 전송
        if (result) {
            budgetService.checkAndNotifyBudgetExceededOnTransaction(memberId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "이체가 성공적으로 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

}
