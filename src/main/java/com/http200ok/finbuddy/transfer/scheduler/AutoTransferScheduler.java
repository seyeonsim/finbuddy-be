//package com.http200ok.finbuddy.transfer.scheduler;
//
//import com.http200ok.finbuddy.transfer.service.AutoTransferService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class AutoTransferScheduler {
//
//    private final AutoTransferService autoTransferService;
//
//    @Scheduled(cron = "0 56 10 * * ?")
//    public void runAutoTransfers() {
//        System.out.println("자동이체 스케줄러 실행됨");
//        autoTransferService.executeScheduledAutoTransfers();
//    }
//}