package com.libera.ticket.service;

import com.libera.ticket.api.dto.*;
import com.libera.ticket.domain.*;
import com.libera.ticket.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final PerformerRepo performerRepo;
    private final ApplicationRepo applicationRepo;
    private final ApplicationMemberRepo memberRepo;
    private final CancelTokenRepo cancelTokenRepo;
    private final SmsService smsService;
    private final ResendEmailService resendEmailService;

    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${app.result-notice-date}")
    private LocalDate resultNoticeDate;
    @Value("${app.cancel-token-expire-days:30}")
    private int tokenExpireDays;


    @Transactional
    public CreateApplicationRes create(CreateApplicationReq req) {
        DomainType domainType = DomainType.valueOf(req.domainType());

        Performer performer = null;
        if (domainType == DomainType.INVITE) {
            performer = performerRepo.findByNameIgnoreCase(req.performerName())
                    .orElseThrow(() -> new IllegalArgumentException("INVALID_PERFORMER"));
        }

        if (req.members().isEmpty()) throw new IllegalArgumentException("NO_MEMBERS");

        var members = req.members();
        boolean repDelivery = req.isRepDelivery();
        if (members.size() < 2) repDelivery = false;

        var rep = members.get(0);
        if (rep.email() == null || rep.phone() == null)
            throw new IllegalArgumentException("REP_CONTACT_REQUIRED");

        var app = new Application();
        app.setDomainType(domainType);
        app.setPerformer(performer);
        app.setRepName(rep.name());
        app.setRepEmail(rep.email());
        app.setRepPhone(rep.phone());
        app.setTotalCount(members.size());
        app.setRepDelivery(repDelivery);
        applicationRepo.save(app);

        for (int i = 0; i < members.size(); i++) {
            var m = members.get(i);
            var am = new ApplicationMember();
            am.setApplication(app);
            am.setRowOrder(i + 1);
            am.setName(m.name());
            boolean relyOnRep = (repDelivery && i > 0);
            if (!relyOnRep) {
                am.setEmail((m.email() == null || m.email().isBlank()) ? null : m.email());
                am.setPhone((m.phone() == null || m.phone().isBlank()) ? null : m.phone());
            }
            memberRepo.save(am);
        }

        var ct = new CancelToken();
        ct.setApplication(app);
        ct.setExpiredAt(LocalDateTime.now().plusDays(tokenExpireDays));
        cancelTokenRepo.save(ct);

        String cancelUrl = baseUrl + "/cancel/" + ct.getToken();
        String msg = "초대권 신청이 정상적으로 완료되었습니다.\n" +
                "확인 메일 및 문자가 발송되었으니, 문자나 메일을 수신하지 못하신 경우 스팸함 확인하신 후 스팸해제 진행 부탁드립니다.";
        // 대표 연락처(1행)로만 발송
        var repEmail = req.members().get(0).email();
        var repPhone = req.members().get(0).phone(); // 010-1234-5678 형태
        // … 기존 저장 로직 후
        try {
            if (domainType == DomainType.INVITE) {
                smsService.sendSms(repPhone, "[Libera] 초대권 신청이 완료되었습니다.");
            }else{
                smsService.sendSms(repPhone, "[Libera] 티켓 응모가 완료되었습니다.");
            }
        } catch (Exception e) {
            log.warn("SMS 발송 실패: {}", e.getMessage());
        }
        try {
            UUID id = app.getApplicationId();

            String viewUrl = baseUrl + "/app/" + id; // ✅ 공개 조회 링크

            if (domainType == DomainType.INVITE) {
                // 메일 HTML (간결한 버튼)
                String subject = "[Libera] 초대권 신청 완료";
                String html = """
                        <div style="font-family:pretendard,Apple SD Gothic Neo,Roboto,Noto Sans,Arial,sans-serif;line-height:1.6;color:#111827">
                          <h2 style="margin:0 0 8px 0;font-size:18px">초대권 신청이 완료되었습니다.</h2>
                          <p style="margin:0 0 16px 0;color:#4b5563">신청 상세와 취소는 아래 버튼으로 확인하실 수 있습니다.</p>
                          <p style="margin:24px 0">
                            <a href="%s" style="display:inline-block;background:#111827;color:#fff;text-decoration:none;
                               padding:12px 18px;border-radius:10px">내 신청내역 보기</a>
                          </p>
                          <p style="margin:24px 0 0 0;font-size:12px;color:#6b7280">
                            본 메일은 발신 전용입니다. 링크가 열리지 않으면 아래 URL을 복사해 브라우저 주소창에 붙여넣어 주세요.<br/>
                            %s
                          </p>
                        </div>
                        """.formatted(viewUrl, viewUrl);
                resendEmailService.sendHtml(repEmail, subject, html);
            }
            else{
                String subject = "[Libera] 티켓 응모 완료";
                String html = """
                        <div style="font-family:pretendard,Apple SD Gothic Neo,Roboto,Noto Sans,Arial,sans-serif;line-height:1.6;color:#111827">
                          <h2 style="margin:0 0 8px 0;font-size:18px">응모가 접수되었습니다.</h2>
                          <p style="margin:0 0 16px 0;color:#4b5563">응모 상세와 취소는 아래 버튼으로 확인하실 수 있습니다.</p>
                          <p style="margin:24px 0">
                            <a href="%s" style="display:inline-block;background:#111827;color:#fff;text-decoration:none;
                               padding:12px 18px;border-radius:10px">내 응모내역 보기</a>
                          </p>
                          <p style="margin:24px 0 0 0;font-size:12px;color:#6b7280">
                            본 메일은 발신 전용입니다. 링크가 열리지 않으면 아래 URL을 복사해 브라우저 주소창에 붙여넣어 주세요.<br/>
                            %s
                          </p>
                        </div>
                        """.formatted(viewUrl, viewUrl);
                resendEmailService.sendHtml(repEmail, subject, html);
            }
        } catch (Exception e) {
            log.warn("이메일 발송 실패: {}", e.getMessage());
        }

        return new CreateApplicationRes(app.getApplicationId().toString(), app.getTotalCount(), cancelUrl, msg);
    }

    @Transactional
    public String cancelByToken(UUID token) {
        var ct = cancelTokenRepo.findById(token).orElseThrow();
        var app = ct.getApplication();
        app.setStatus(AppStatus.CANCELLED);
        applicationRepo.save(app);
        return "응모가 취소되었습니다.";
    }

    @Transactional
    public String cancelById(UUID applicationId){
        var app = applicationRepo.findById(applicationId).orElseThrow();
        app.setStatus(AppStatus.CANCELLED);
        applicationRepo.save(app);
        return "응모가 취소되었습니다.";
    }

    @Transactional
    public String cancelByApplicationId(UUID applicationId) {
        var app = applicationRepo.findById(applicationId).orElseThrow();
        app.setStatus(AppStatus.CANCELLED);
        applicationRepo.save(app);
        return "응모가 취소되었습니다.";
    }

}
