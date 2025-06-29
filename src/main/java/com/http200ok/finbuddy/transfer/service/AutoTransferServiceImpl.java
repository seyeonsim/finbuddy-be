package com.http200ok.finbuddy.transfer.service;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.account.repository.AccountRepository;
import com.http200ok.finbuddy.common.validator.AccountValidator;
import com.http200ok.finbuddy.notification.domain.NotificationType;
import com.http200ok.finbuddy.notification.service.NotificationService;
import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.transfer.domain.AutoTransferStatus;
import com.http200ok.finbuddy.transfer.dto.AutoTransferUpdateRequestDto;
import com.http200ok.finbuddy.transfer.repository.AutoTransferRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoTransferServiceImpl implements AutoTransferService {
    private final AccountRepository accountRepository;
    private final AutoTransferRepository autoTransferRepository;
    private final AccountValidator accountValidator;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AutoTransfer createAutoTransfers(Long fromAccountId, String bankName, String targetAccountNumber, Long amount, Integer transferDay) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new EntityNotFoundException("선택하신 출금 계좌를 찾을 수 없습니다."));

        // 입력한 입금 계좌 조회
        Account toAccount = accountValidator.validateAndGetBankAccount(bankName, targetAccountNumber);

        // 자동이체 엔티티 생성
        AutoTransfer autoTransfer = AutoTransfer.createAutoTransfer(
                fromAccount,
                toAccount.getBank().getName(),
                toAccount.getAccountNumber(),
                amount,
                transferDay
        );

        return autoTransferRepository.save(autoTransfer);
    }

    // 특정 회원의 자동이체 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<AutoTransfer> getAutoTransfersByMember(Long memberId) {
        return autoTransferRepository.findByAccount_Member_Id(memberId);
    }

    // 자동이체 정보 조회
    @Transactional(readOnly = true)
    public AutoTransfer getAutoTransferById(Long autoTransferId) {
        return autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 정보를 찾을 수 없습니다."));
    }

    // 자동이체 정보 수정(금액 날짜)
    @Override
    @Transactional
    public void updateAutoTransfer(Long autoTransferId, AutoTransferUpdateRequestDto requestDto) {
        AutoTransfer autoTransfer = autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 정보를 찾을 수 없습니다."));

        autoTransfer.updateTransferInfo(requestDto.getAmount(), requestDto.getTransferDay());
    }

    // 자동이체 상태 변경
    @Override
    @Transactional
    public void toggleAutoTransferStatus(Long autoTransferId) {
        AutoTransfer autoTransfer = autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 정보를 찾을 수 없습니다."));

        autoTransfer.toggleActiveStatus();
    }

    @Override
    @Transactional
    public void deleteAutoTransfer(Long autoTransferId) {
        AutoTransfer autoTransfer = autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 정보가 존재하지 않습니다."));

        autoTransferRepository.delete(autoTransfer);
    }

    // 스케줄러
//    @Transactional
//    public void executeScheduledAutoTransfers() {
//        int today = LocalDate.now().getDayOfMonth();
//        System.out.println("자동이체 실행 - 오늘 날짜: " + today);
//
//        List<AutoTransfer> transfers = autoTransferRepository.findByTransferDayAndStatus(today, AutoTransferStatus.ACTIVE);
//
//        if (transfers.isEmpty()) {
//            System.out.println("오늘 실행할 자동이체가 없습니다.");
//            return;
//        }
//
//        for (AutoTransfer transfer : transfers) {
//            try {
//                boolean success = transferService.executeAccountTransfer(
//                        transfer.getAccount().getMember().getId(),
//                        transfer.getAccount().getId(),
//                        transfer.getTargetBankName(),
//                        transfer.getTargetAccountNumber(),
//                        transfer.getAmount(),
//                        transfer.getAccount().getPassword(),
//                        transfer.getAccount().getMember().getName(),
//                        null
//                );
//
//                if (!success) {
//                    System.out.println("자동이체 성공 (ID: " + transfer.getId() + ")");
//                } else {
//                    System.out.println("자동이체 실패 (ID: " + transfer.getId() + ")");
//                }
//
//            } catch (Exception e) {
//                System.out.println("자동이체 중 오류 발생 (ID: " + transfer.getId() + "): " + e.getMessage());
//            }
//        }
//    }

    /**
     * 자동이체 실패 처리 및 알림 발송
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailedAndSave(AutoTransfer transfer) {
        transfer.markAsFailed();
        autoTransferRepository.save(transfer);
        sendFailureNotification(transfer);
    }

    /**
     * 자동이체 성공 처리 및 알림 발송
     */
    @Override
    @Transactional
    public void markAsSuccessAndNotify(AutoTransfer transfer) {
        transfer.markAsActive(); // 실패 상태에서 성공으로 변경 가능하도록
        autoTransferRepository.save(transfer);
        sendSuccessNotification(transfer);
    }

    /**
     * 자동이체 성공 알림
     */
    private void sendSuccessNotification(AutoTransfer transfer) {
        String message = String.format(
                "자동이체 성공 안내\n\n" +
                        "출금 계좌: %s\n" +
                        "입금 계좌: %s %s\n" +
                        "이체 금액: %,d원\n" +
                        "이체일: %d",
                transfer.getAccount().getAccountNumber(),
                transfer.getTargetBankName(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getTransferDay()
        );
        notificationService.sendNotification(transfer.getAccount().getMember(), NotificationType.AUTOTRANSFERSUCCESS, message);
    }

    /**
     * 자동이체 실패 알림
     */
    private void sendFailureNotification(AutoTransfer transfer) {
        String message = String.format(
                "자동이체 실패 안내\n\n" +
                        "출금 계좌: %s\n" +
                        "입금 계좌: %s %s\n" +
                        "이체 금액: %,d원\n" +
                        "사유: 잔액 부족 또는 기타 오류",
                transfer.getAccount().getAccountNumber(),
                transfer.getTargetBankName(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount()
        );
        notificationService.sendNotification(transfer.getAccount().getMember(), NotificationType.AUTOTRANSFERFAIL, message);
    }
}
