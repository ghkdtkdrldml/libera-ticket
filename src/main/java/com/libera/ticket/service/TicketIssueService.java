// com/libera/ticket/service/TicketIssueService.java
package com.libera.ticket.service;

import com.libera.ticket.domain.*;
import com.libera.ticket.repo.ApplicationMemberRepo;
import com.libera.ticket.repo.ApplicationRepo;
import com.libera.ticket.repo.TicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TicketIssueService {

    private final TicketRepo ticketRepo;
    private final ApplicationRepo applicationRepo;
    private final ApplicationMemberRepo memberRepo;
    private final QrService qrService;
    private final ResendEmailService emailService;  // Resend 연동
    private final SmsService smsService;            // 쿨SMS 연동

    @Value("${app.base-url}")
    private String baseUrl;

    /** 어드민에서 호출: 응모 ID 기준 발급 (이미 발급된 건 있으면 즉시 반환) */
    @Transactional
    public int issueForApplicationId(UUID applicationId) {
        long exists = ticketRepo.countByApplication_ApplicationId(applicationId);
        if (exists > 0) {
            return 0; // 이미 발급된 상태 -> 중복 방지
        }

        Application app = applicationRepo.findById(applicationId).orElseThrow();
        List<ApplicationMember> members =
                memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId);

        int issued = 0;
        int row = 0;
        for (ApplicationMember m : members) {
            row++;
            Ticket t = new Ticket();
            t.setApplication(app);
            t.setMemberRow(row);
            t.setMemberName(m.getName());
            t.setMemberEmail(m.getEmail());
            t.setMemberPhone(m.getPhone());
            t.setToken(generateToken());
            t.setStatus(TicketStatus.ISSUED);
            t.setIssuedAt(OffsetDateTime.now());
            ticketRepo.save(t);

            // 링크 & QR (메일 본문용 Base64)
            String viewUrl = baseUrl + "/t/" + t.getToken();
            String qrImgDataUri = "data:image/png;base64," + base64Qr(viewUrl, 360);

            // 이메일 발송 (있을 때만)
            if (t.getMemberEmail() != null && !t.getMemberEmail().isBlank()) {
                String subject = "리베라 공연 입장 QR 티켓";
                String html = """
                    <div style="font-family:system-ui,Apple SD Gothic Neo,Noto Sans,Arial,sans-serif;color:#111827">
                      <h2 style="margin:0 0 8px 0;font-size:18px">입장 티켓</h2>
                      <p style="margin:0 0 12px 0;color:#6b7280">현장에서 아래 QR을 제시해 주세요.</p>
                      <p style="margin:16px 0"><img src="%s" alt="QR Ticket" style="width:220px;height:220px;border-radius:8px;border:1px solid #eee"></p>
                      <p style="margin:12px 0">
                        <a href="%s" style="display:inline-block;background:#1f2937;color:#fff;text-decoration:none;padding:10px 14px;border-radius:10px">티켓 상세 보기</a>
                      </p>
                      <p style="margin:16px 0 0 0;font-size:12px;color:#6b7280">링크가 열리지 않으면 다음 주소를 복사해 브라우저에 붙여 넣어 주세요.<br>%s</p>
                    </div>
                """.formatted(qrImgDataUri, viewUrl, viewUrl);
                try { emailService.sendHtml(t.getMemberEmail(), subject, html); } catch (Exception ignored) {}
            }

            // 문자(선택)
            if (t.getMemberPhone() != null && !t.getMemberPhone().isBlank()) {
                try { smsService.sendSms(t.getMemberPhone(), "[리베라] 입장 QR 티켓: " + viewUrl); } catch (Exception ignored) {}
            }
            issued++;
        }
        return issued;
    }

    private String generateToken(){
        // UUID + 랜덤 64비트 → 32~36자 이상 난수
        return UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    private String base64Qr(String content, int size){
        try(var baos = new ByteArrayOutputStream()){
            ImageIO.write(qrService.generatePng(content, size), "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
