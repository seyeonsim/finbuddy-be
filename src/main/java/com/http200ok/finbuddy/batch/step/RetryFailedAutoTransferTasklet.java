package com.http200ok.finbuddy.batch.step;

import com.http200ok.finbuddy.transfer.domain.AutoTransfer;
import com.http200ok.finbuddy.transfer.domain.AutoTransferStatus;
import com.http200ok.finbuddy.transfer.repository.AutoTransferRepository;
import com.http200ok.finbuddy.transfer.service.AutoTransferService;
import com.http200ok.finbuddy.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RetryFailedAutoTransferTasklet implements Tasklet {

    private final AutoTransferRepository autoTransferRepository;
    private final TransferService transferService;
    private final AutoTransferService autoTransferService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<AutoTransfer> failedTransfers = autoTransferRepository.findByStatus(AutoTransferStatus.FAILED);

        if (failedTransfers.isEmpty()) {
            System.out.println("재시도할 실패한 자동이체 없음.");
            return RepeatStatus.FINISHED;
        }

        for (AutoTransfer transfer : failedTransfers) {
            try {
                boolean success = transferService.autoExecuteAccountTransfer(
                        transfer.getAccount().getMember().getId(),
                        transfer.getAccount().getId(),
                        transfer.getTargetBankName(),
                        transfer.getTargetAccountNumber(),
                        transfer.getAmount(),
                        transfer.getAccount().getMember().getName(),
                        null
                );

                if (success) {
                    autoTransferService.markAsSuccessAndNotify(transfer);
                    System.out.println("자동이체 재시도 성공 ID: " + transfer.getId());
                } else {
                    System.out.println("자동이체 재시도 실패 ID: " + transfer.getId());
                }
            } catch (Exception e) {
                System.out.println("자동이체 재시도 중 오류 발생 ID " + transfer.getId() + e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
