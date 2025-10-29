package com.libera.ticket.test;

import com.libera.ticket.domain.Application;
import com.libera.ticket.domain.DomainType;
import com.libera.ticket.repo.ApplicationRepo;
import com.libera.ticket.domain.Channel;
import com.libera.ticket.domain.NotificationKind;
import com.libera.ticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/test")
@RequiredArgsConstructor
public class NotificationTestController {

    private final ApplicationRepo applicationRepository;
    private final NotificationService notificationService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.event.wonnam}")
    private String wonnamImage; // 정적 파일 경로 or 절대 URL

    /**
     * 특정 applicationId 에 대해 "확정 메일"만 발송 (상태 변경 X)
     * 예) POST /internal/test/confirm-email/4b7d3c9e-...-...  (?alsoSms=true)
     */
    @PostMapping("/confirm-email/{applicationId}")
    public ResponseEntity<?> sendConfirmEmail(
            @PathVariable String applicationId,
            @RequestParam(defaultValue = "true") boolean alsoSms
    ) {
        UUID id = UUID.fromString(applicationId);
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("APPLICATION_NOT_FOUND"));

        // 확정 알림 템플릿 선택 (초대권/응모)
        NotificationKind kind = (app.getDomainType() == DomainType.INVITE)
                ? NotificationKind.APPLICATION_CONFIRMED
                : NotificationKind.ENTRY_CONFIRMED;

        String viewUrl = baseUrl + "/app/" + id; // ✅ 공개 조회 링크

        // 메일/문자 템플릿에서 쓰는 변수들
        Map<String, Object> model = new HashMap<>();
        model.put("eventDateTimeText", "11월 8일(토) 저녁 7시");
        model.put("venueName", "원남교당 2층 대각전");
        model.put("qrSendDateText", "11월 7일");
        model.put("venueFullAddress", "서울시 종로구 창경궁로 22길 22-2");
        model.put("detailUrl", viewUrl);
        model.put("photoUrl", baseUrl+"/"+wonnamImage);
        // 필요 시: model.put("cancelUrl", "https://.../cancel/xxxxx");

        // 수신자
        String email = app.getRepEmail();
        String phone = app.getRepPhone();

        // 메일 발송
        notificationService.send(kind, Channel.EMAIL, email, new HashMap<>(model));

        // 옵션: 문자도 같이
        if (alsoSms) {
            notificationService.send(kind, Channel.SMS, phone, new HashMap<>(model));
        }

        return ResponseEntity.ok(Map.of(
                "applicationId", applicationId,
                "email", email,
                "sms", alsoSms ? phone : null,
                "kind", kind.name(),
                "status", "SENT"
        ));
    }
}
