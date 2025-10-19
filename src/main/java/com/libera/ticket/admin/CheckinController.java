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

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/checkin")
@PreAuthorize("hasRole('ADMIN')")
public class CheckinController {
    private final TicketRepo ticketRepo;

    @GetMapping("")
    public String page(){ return "admin_checkin"; }

    @PostMapping("/{token}")
    @ResponseBody
    public ResponseEntity<?> checkin(@PathVariable String token){
        var t = ticketRepo.findByToken(token).orElse(null);
        if(t==null) return ResponseEntity.status(404).body("INVALID");
        if(t.getStatus()== TicketStatus.USED) return ResponseEntity.ok("ALREADY:" + t.getUsedAt());
        if(t.getStatus()== TicketStatus.CANCELLED) return ResponseEntity.ok("CANCELLED");
        t.setStatus(TicketStatus.USED);
        t.setUsedAt(OffsetDateTime.now());
        ticketRepo.save(t);
        return ResponseEntity.ok("OK");
    }
}
