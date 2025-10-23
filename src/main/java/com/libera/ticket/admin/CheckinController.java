// com/libera/ticket/admin/CheckinController.java
package com.libera.ticket.admin;

import com.libera.ticket.domain.TicketStatus;
import com.libera.ticket.repo.TicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/checkin")
@PreAuthorize("hasRole('ADMIN')")
public class CheckinController {
    private final TicketRepo ticketRepo;

    @GetMapping("")
    public String page(){ return "admin_checkin"; }

    // ✅ 추가: 스캔 후 정보 조회(토큰으로 조회 → JSON 반환)
    @GetMapping("/ticket/{token}")
    @ResponseBody
    public ResponseEntity<?> getTicket(@PathVariable String token){
        var t = ticketRepo.findByToken(token).orElse(null);
        if(t == null){
            return ResponseEntity.status(404).body(new TicketResp("NOT_FOUND", "유효하지 않은 티켓입니다.", null, null, null, null, null));
        }
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String issued = t.getIssuedAt()!=null ? t.getIssuedAt().format(fmt) : null;
        String used   = t.getUsedAt()!=null   ? t.getUsedAt().format(fmt)   : null;

        // 이메일/전화번호는 마스킹(현장 노출 최소화)
        String emailMasked = maskEmail(t.getMemberEmail());
        String phoneMasked = maskPhone(t.getMemberPhone());

        String status = switch (t.getStatus()){
            case ISSUED -> "ISSUED";
            case USED -> "USED";
            case CANCELED -> "CANCELED";
        };

        var resp = new TicketResp(
                status,
                statusText(status),
                t.getMemberName(),
                emailMasked,
                phoneMasked,
                issued,
                used
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{token}")
    @ResponseBody
    public ResponseEntity<?> checkin(@PathVariable String token){
        var t = ticketRepo.findByToken(token).orElse(null);
        if(t==null) return ResponseEntity.status(404).body("INVALID");
        if(t.getStatus()== TicketStatus.USED) return ResponseEntity.ok("ALREADY:" + t.getUsedAt());
        if(t.getStatus()== TicketStatus.CANCELED) return ResponseEntity.ok("CANCELED");
        t.setStatus(TicketStatus.USED);
        t.setUsedAt(OffsetDateTime.now());
        ticketRepo.save(t);
        return ResponseEntity.ok("OK");
    }


    // ---------- helpers ----------

    private static String maskEmail(String email){
        if(email==null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if(at <= 1) return "***" + email.substring(at);
        String head = email.substring(0, Math.min(3, at));
        return head + "***" + email.substring(at);
    }

    private static String maskPhone(String phone){
        if(phone==null || phone.isBlank()) return null;
        // 010-1234-5678 -> 010-****-5678
        return phone.replaceFirst("^(\\d{3}-)\\d{3,4}(-\\d{4})$", "$1****$2");
    }

    private static String statusText(String s){
        return switch (s){
            case "ISSUED" -> "미사용";
            case "USED" -> "사용됨";
            case "CANCELED" -> "취소됨";
            default -> "알수없음";
        };
    }

    // 간단 DTO (record 사용 가능)
    public record TicketResp(
            String code,        // ISSUED/USED/CANCELED/NOT_FOUND
            String message,     // 미사용/사용됨/취소됨/유효하지 않음
            String name,
            String email,
            String phone,
            String issuedAt,
            String usedAt
    ) {}
}
