// com/libera/ticket/service/TicketIssueBatchService.java
package com.libera.ticket.service;

import com.libera.ticket.domain.AppStatus;
import com.libera.ticket.repo.ApplicationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketIssueBatchService {

    private final ApplicationRepo applicationRepo;
    private final TicketIssueService ticketIssueService;

    /** 확정 상태 전체에 대해 발급 수행 */
    public BatchIssueResult issueAllConfirmed() {
        var ids = applicationRepo.findIdsByStatus(AppStatus.CONFIRMED);
        int totalTickets = 0;
        Map<UUID, Integer> details = new LinkedHashMap<>();

        for (var appId : ids) {
            try {
                int issued = ticketIssueService.issueForApplicationId(appId);
                details.put(appId, issued);
                totalTickets += issued;
                if (issued > 0) {
                    log.info("Ticket issued: appId={}, count={}", appId, issued);
                }
            } catch (Exception e) {
                log.warn("Ticket issue failed: appId={}, err={}", appId, e.toString());
                details.put(appId, -1); // 실패 표시
            }
        }
        return new BatchIssueResult(ids.size(), totalTickets, details);
    }

    /** 결과 요약 DTO */
    public record BatchIssueResult(int totalAppsTried, int totalTicketsIssued, Map<UUID, Integer> perApp) {}
}
