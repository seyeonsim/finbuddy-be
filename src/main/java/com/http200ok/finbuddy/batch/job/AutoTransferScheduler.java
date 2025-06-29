package com.http200ok.finbuddy.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class AutoTransferScheduler {

    private final JobLauncher jobLauncher;
    private final Job autoTransferJob;
    private final Job retryFailedAutoTransferJob;

    public AutoTransferScheduler(JobLauncher jobLauncher,
                                 @Qualifier("autoTransferJob") Job autoTransferJob,
                                 @Qualifier("retryFailedAutoTransferJob") Job retryFailedAutoTransferJob) {
        this.jobLauncher = jobLauncher;
        this.autoTransferJob = autoTransferJob;
        this.retryFailedAutoTransferJob = retryFailedAutoTransferJob;
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void runAutoTransferJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(autoTransferJob, params);
            System.out.println("Batch 자동이체 Job 실행됨");
        } catch (Exception e) {
            System.out.println("Batch 자동이체 Job 실행 중 오류" + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void runRetryFailedAutoTransferJob() {
        LocalTime now = LocalTime.now();

        // 오전 9시 이전이거나 오후 6시 이후에는 실행하지 않음
        if (now.isBefore(LocalTime.of(9, 0)) || now.isAfter(LocalTime.of(18, 0))) {
            System.out.println("현재 시간이 오전 9시 이전 또는 오후 6시 이후이므로 자동이체 재시도를 중단합니다.");
            return;
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(retryFailedAutoTransferJob, params);
            System.out.println("실패한 자동이체 Job 실행됨");
        } catch (Exception e) {
            System.out.println("실패한 자동이체 Job 실행 중 오류" + e.getMessage());
        }
    }
}
