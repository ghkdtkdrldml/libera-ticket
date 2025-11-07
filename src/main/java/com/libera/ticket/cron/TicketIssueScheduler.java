package com.libera.ticket.cron;


import com.libera.ticket.service.TicketIssueBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketIssueScheduler {

    private final TicketIssueBatchService batchService;

    // ✅ 예시 1) 11/07 18:00 KST 단발 실행
    @Scheduled(cron = "0 0 18 7 11 *", zone = "Asia/Seoul")
    public void issueOnceNov07() {
        var r = batchService.issueAllConfirmed();
        log.info("[TicketIssueScheduler] Nov07 result: appsTried={}, ticketsIssued={}",
                r.totalAppsTried(), r.totalTicketsIssued());
    }

}