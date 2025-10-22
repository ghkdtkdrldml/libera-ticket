package com.libera.ticket.api;

import com.libera.ticket.api.dto.*;
import com.libera.ticket.domain.*;
import com.libera.ticket.repo.*;
import com.libera.ticket.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final PerformerRepo performerRepo;
    private final ApplicationRepo appRepo;
    private final ApplicationMemberRepo memberRepo;
    private final ApplicationService service;

    @Value("${app.event.poster:/img/Libera_poster_resized.jpg}")
    private String poster; // 정적 파일 경로 or 절대 URL

    @Value("${app.event.program_image:/img/Libera_program_resized.jpg}")
    private String programImage; // 정적 파일 경로 or 절대 URL

    @GetMapping({"/", "/main"})
    public String main(Model model) {
        model.addAttribute("poster", poster);                 // app.event.poster
        model.addAttribute("programImage", programImage);     // app.event.program_image
        return "index";
    }
//
//    // 공지 페이지
//    @GetMapping("/")
//    public String notice() {
//        return "notice";
//    }
//
//    // 기존 메인 페이지
//    @GetMapping("/main")
//    public String main() {
//        return "index"; // 기존 메인 페이지 템플릿 이름
//    }

    @GetMapping("/rsvp")
    public String rsvp() {
        return "rsvp";
    }

    @GetMapping("/invite")
    public String invite(Model model) {
        model.addAttribute("performers", performerRepo.findAll());
        return "invite";
    }

    // API
    @GetMapping("/api/performers")
    @ResponseBody
    public List<Performer> performers() {
        return performerRepo.findAll();
    }

    @PostMapping("/api/applications")
    @ResponseBody
    public CreateApplicationRes create(@Valid @RequestBody CreateApplicationReq req) {
        return service.create(req);
    }

    @PostMapping("/api/cancel/{token}")
    @ResponseBody
    public Map<String, String> cancel(@PathVariable UUID token) {
        String msg = service.cancelByToken(token);
        return Map.of("status", "CANCELLED", "message", msg);
    }

    @PostMapping("/cancel/{applicationId}")
    @ResponseBody
    public Map<String, String> cancelById(@PathVariable UUID applicationId) {
        String msg = service.cancelById(applicationId);
        return Map.of("status", "CANCELLED", "message", msg);
    }

    // CSV Export
    @GetMapping(value = "/api/export/csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> csv() {
        StringBuilder sb = new StringBuilder();
        sb.append("application_id,domain_type,performer,created_at,status,row_order,name,email,phone\n");
        for (Application a : appRepo.findAll()) {
            var members = memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(a.getApplicationId());
            for (ApplicationMember m : members) {
                sb.append(a.getApplicationId()).append(',')
                        .append(a.getDomainType().ko()).append(',')
                        .append(a.getPerformer() == null ? "" : a.getPerformer().getName()).append(',')
                        .append(a.getCreatedAt()).append(',')
                        .append(a.getStatus().ko()).append(',')
                        .append(m.getRowOrder()).append(',')
                        .append(q(m.getName())).append(',')
                        .append(q(m.getEmail())).append(',')
                        .append(q(m.getPhone())).append('\n');
            }
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications.csv")
                .body(bytes);
    }

    private String q(String s) {
        return s == null ? "" : ('"' + s.replace("\"", "\"\"") + '"');
    }
}
