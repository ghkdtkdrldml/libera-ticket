package com.libera.ticket.service;

import com.libera.ticket.api.dto.*;
import com.libera.ticket.domain.*;
import com.libera.ticket.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final PerformerRepo performerRepo;
    private final ApplicationRepo applicationRepo;
    private final ApplicationMemberRepo memberRepo;
    private final CancelTokenRepo cancelTokenRepo;

    @Value("${app.base-url}") private String baseUrl;
    @Value("${app.result-notice-date}") private LocalDate resultNoticeDate;
    @Value("${app.cancel-token-expire-days:30}") private int tokenExpireDays;

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

        for (int i=0;i<members.size();i++){
            var m = members.get(i);
            var am = new ApplicationMember();
            am.setApplication(app);
            am.setRowOrder(i+1);
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
        String msg = "응모가 접수되었습니다. " + resultNoticeDate + "에 결과를 발송할 예정입니다.";
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
}
