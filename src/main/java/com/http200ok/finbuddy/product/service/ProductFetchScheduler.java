package com.http200ok.finbuddy.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductFetchScheduler {

    private final ProductFetchWebClientService productFetchWebClientService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void fetchDepositProductSchedule() {
        productFetchWebClientService.fetchAndSaveProducts("deposit")
                .subscribe();  // 실행 시작
    }

}
