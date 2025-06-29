package com.http200ok.finbuddy.transfer.service;

import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.transfer.dto.AutoTransferUpdateRequestDto;

import java.util.List;

public interface AutoTransferService {
    AutoTransfer createAutoTransfers(Long fromAccountId, String bankName, String targetAccountNumber, Long amount, Integer transferDay);
    List<AutoTransfer> getAutoTransfersByMember(Long memberId);

    AutoTransfer getAutoTransferById(Long autoTransferId);
    void updateAutoTransfer(Long autoTransferId, AutoTransferUpdateRequestDto requestDto);
    void toggleAutoTransferStatus(Long autoTransferId);
    void deleteAutoTransfer(Long autoTransferId);
//    void executeScheduledAutoTransfers();
    void markAsSuccessAndNotify(AutoTransfer transfer);
    void markAsFailedAndSave(AutoTransfer transfer);

}
