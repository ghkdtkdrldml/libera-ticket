package com.libera.ticket.service;

import com.libera.ticket.domain.*;
import com.libera.ticket.repo.ApplicationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmService {

    private final ApplicationRepo applicationRepo;
    private final NotificationService notificationService;

    @Transactional
    public void confirmAllSubmitted() {
        List<Application> targets = applicationRepo.findAllSubmittedForUpdate();
        if (targets.isEmpty()) {
            log.info("[ConfirmBatch] no SUBMITTED applications to confirm");
            return;
        }

        int ok = 0, fail = 0;
        for (Application app : targets) {
            try {
                // 1) 상태 전환
                app.setStatus(AppStatus.CONFIRMED);

                // 2) 알림 발송 (확정용 템플릿)
                Map<String, Object> model = new HashMap<>();
                model.put("eventDateTimeText", "11월 8일(토) 저녁 7시");
                model.put("venueName", "원남교당 2층 대각전");
                model.put("qrSendDateText", "11월 7일");
                model.put("venueFullAddress", "서울시 종로구 창경궁로 22길 22-2");
                // 필요 시 취소링크/사진 등 추가

                NotificationKind kind = (app.getDomainType() == DomainType.INVITE)
                        ? NotificationKind.APPLICATION_CONFIRMED
                        : NotificationKind.ENTRY_CONFIRMED;

                // 대표 연락처
                String email = app.getRepEmail();
                String phone = app.getRepPhone();

                notificationService.send(kind, Channel.EMAIL, email, new HashMap<>(model));
                notificationService.send(kind, Channel.SMS,   phone, new HashMap<>(model));

                ok++;
            } catch (Exception e) {
                log.warn("[ConfirmBatch] confirm/send failed for appId={}, err={}",
                        app.getApplicationId(), e.toString());
                fail++;
            }
        }
        log.info("[ConfirmBatch] done: confirmed={}, failed={}", ok, fail);
    }
}