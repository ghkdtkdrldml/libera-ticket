package com.libera.ticket.publicview;

import com.libera.ticket.repo.ApplicationMemberRepo;
import com.libera.ticket.repo.ApplicationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PublicController {
    private final ApplicationRepo appRepo;
    private final ApplicationMemberRepo memberRepo;

    @GetMapping("/app/{id}")
    public String view(@PathVariable("id") UUID id, Model model){
        var app = appRepo.findById(id).orElse(null);
        if(app == null){
            model.addAttribute("notfound", true);
            return "app_view";
        }
        var members = memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(id);
        model.addAttribute("app", app);
        model.addAttribute("members", members);
        return "app_view";
    }
}
