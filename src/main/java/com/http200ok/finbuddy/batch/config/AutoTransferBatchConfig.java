package com.http200ok.finbuddy.batch.config;

import com.http200ok.finbuddy.batch.step.AutoTransferTasklet;
import com.http200ok.finbuddy.batch.step.RetryFailedAutoTransferTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class AutoTransferBatchConfig {

    private final JobRepository jobRepository;
    private final AutoTransferTasklet autoTransferTasklet;
    private final RetryFailedAutoTransferTasklet retryFailedAutoTransferTasklet;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job autoTransferJob() {
        return new JobBuilder("autoTransferJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(autoTransferStep())
                .build();
    }

    @Bean
    public Step autoTransferStep() {
        return new StepBuilder("autoTransferStep", jobRepository)
                .tasklet(autoTransferTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job retryFailedAutoTransferJob() {
        return new JobBuilder("retryFailedAutoTransferJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(retryFailedAutoTransferStep())
                .build();
    }

    @Bean
    public Step retryFailedAutoTransferStep() {
        return new StepBuilder("retryFailedAutoTransferStep", jobRepository)
                .tasklet(retryFailedAutoTransferTasklet, transactionManager)
                .build();
    }
}