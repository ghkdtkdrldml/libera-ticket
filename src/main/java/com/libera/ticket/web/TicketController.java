// com/libera/ticket/web/TicketController.java
package com.libera.ticket.web;

import com.libera.ticket.domain.TicketStatus;
import com.libera.ticket.repo.TicketRepo;
import com.libera.ticket.service.QrService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;

@Controller
@RequiredArgsConstructor
public class TicketController {
    private final TicketRepo ticketRepo;
    private final QrService qr;

    // 티켓 상세 (현장/사용자 공용 뷰)
    @GetMapping("/t/{token}")
    public String view(@PathVariable String token, Model model){
        var t = ticketRepo.findByToken(token).orElse(null);
        if(t==null){ model.addAttribute("notfound", true); return "ticket_view"; }
        model.addAttribute("t", t);
        model.addAttribute("used", t.getStatus()== TicketStatus.USED);
        return "ticket_view";
    }

    // QR 이미지 직접 출력(메일에서 <img src>로 쓰고 싶을 때)
    @GetMapping("/t/q/{token}.png")
    public void qrPng(@PathVariable String token, HttpServletResponse res) throws Exception{
        var url = "/t/" + token;
        res.setContentType("image/png");
        var img = qr.generatePng(url, 320);
        ImageIO.write(img, "png", res.getOutputStream());
    }
}
