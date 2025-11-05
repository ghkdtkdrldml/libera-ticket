// com/libera/ticket/service/TicketIssueService.java
package com.libera.ticket.service;

import com.libera.ticket.domain.*;
import com.libera.ticket.repo.ApplicationMemberRepo;
import com.libera.ticket.repo.ApplicationRepo;
import com.libera.ticket.repo.TicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketIssueService {

    private final TicketRepo ticketRepo;
    private final ApplicationRepo applicationRepo;
    private final ApplicationMemberRepo memberRepo;
    private final QrService qrService;
    private final NotificationService notificationService;

    private final Clock clock = Clock.systemUTC();

    @Value("${app.base-url}")
    private String baseUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 어드민에서 호출: 응모 ID 기준 발급 */
    @Transactional
    public int issueForApplicationId(UUID applicationId) {
        Application app = applicationRepo.findByIdForUpdate(applicationId)
                .orElseGet(() -> applicationRepo.findById(applicationId).orElseThrow());

        List<ApplicationMember> members =
                memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId);
        if (members.isEmpty()) return 0;

        boolean repDelivery = app.isRepDelivery();
        List<Ticket> existing = ticketRepo.findAllByApplication_ApplicationIdOrderByMemberRowAsc(applicationId);

        if (repDelivery) {
            if (!existing.isEmpty()) return 0;
            ApplicationMember rep = members.get(0); // 대표
            issueOneTicketForRecipient(app, rep, 1);
            return 1;
        } else {
            Set<Integer> issuedRows = existing.stream()
                    .map(Ticket::getMemberRow).collect(Collectors.toSet());
            int issued = 0;
            int row = 0;
            for (ApplicationMember m : members) {
                row++;
                if (issuedRows.contains(row)) continue;
                issueOneTicketForRecipient(app, m, row);
                issued++;
            }
            return issued;
        }
    }

    /* ==================== 티켓 1장 발급 + 알림 ==================== */

    private void issueOneTicketForRecipient(Application app, ApplicationMember member, int rowNumber) {
        Ticket t = new Ticket();
        t.setApplication(app);
        t.setMemberRow(rowNumber);
        t.setMemberName(member.getName());
        t.setMemberEmail(safeTrim(member.getEmail()));
        t.setMemberPhone(normalizePhone(member.getPhone()));
        t.setToken(generateToken());
        t.setStatus(TicketStatus.ISSUED);
        t.setIssuedAt(OffsetDateTime.now(clock));
        ticketRepo.save(t);

        String ticketUrl = buildTicketUrl(t.getToken());
//        String qrDataUri = dataUriQrPng(ticketUrl, 480);

        String qrImgUrl  = buildQrImgUrl(t.getToken(), 480);

        // ✅ 최소 모델만 구성
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", t.getMemberName());
        model.put("ticketUrl", ticketUrl);
//        model.put("qrDataUri", qrDataUri);
        model.put("qrImgUrl", qrImgUrl);

        // 이메일
        if (isEmail(t.getMemberEmail())) {
            try {
                notificationService.send(
                        NotificationKind.TICKET_SENT,
                        Channel.EMAIL,
                        t.getMemberEmail(),
                        model
                );
            } catch (Exception e) {
                log.warn("Email send failed: appId={}, email={}, err={}",
                        app.getApplicationId(), maskEmail(t.getMemberEmail()), e.toString());
            }
        }
        // 문자
        if (isPhone(t.getMemberPhone())) {
            try {
                notificationService.send(
                        NotificationKind.TICKET_SENT,
                        Channel.SMS,
                        t.getMemberPhone(),
                        model
                );
            } catch (Exception e) {
                log.warn("SMS send failed: appId={}, phone={}, err={}",
                        app.getApplicationId(), maskPhone(t.getMemberPhone()), e.toString());
            }
        }
    }

    /* ==================== 헬퍼 ==================== */

    private String buildTicketUrl(String token) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("t", token)
                .build()
                .toUriString();
    }

    private String generateToken() {
        byte[] buf = new byte[24];
        SECURE_RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String dataUriQrPng(String content, int size) {
        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(qrService.generatePng(content, size), "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQrImgUrl(String token, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("qr", token + ".png")
                .queryParam("s", size)
                .build()
                .toUriString();
    }



    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizePhone(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private static boolean isEmail(String s) { return s != null && s.contains("@"); }
    private static boolean isPhone(String s) { return s != null && s.length() >= 9; }

    private static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.length() <= 4 ? "***" : phone.substring(0, phone.length() - 4) + "****";
    }
}
