package com.libera.ticket.cron;

import com.libera.ticket.service.ConfirmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmScheduler {

    private final ConfirmService confirmService;

    // ✅ 10/31 17:00 KST 1회 실행
    @Scheduled(cron = "0 0 17 31 10 *", zone = "Asia/Seoul")
    public void runOct31() { confirmService.confirmAllSubmitted(); }

    // ✅ 11/1~11/6 매일 17:00 KST
    @Scheduled(cron = "0 0 17 1-6 11 *", zone = "Asia/Seoul")
    public void runNov01to06() { confirmService.confirmAllSubmitted(); }

}