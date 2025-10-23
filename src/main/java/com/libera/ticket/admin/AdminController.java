package com.libera.ticket.admin;

import com.libera.ticket.domain.AppStatus;
import com.libera.ticket.domain.DomainType;
import com.libera.ticket.repo.ApplicationMemberRepo;
import com.libera.ticket.repo.ApplicationRepo;
import com.libera.ticket.repo.TicketRepo;
import com.libera.ticket.service.ApplicationService;
import com.libera.ticket.service.TicketIssueService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ApplicationRepo appRepo;
    private final ApplicationMemberRepo memberRepo;
    private final TicketRepo ticketRepo;
    private final ApplicationService service;
    private final TicketIssueService ticketIssue;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DomainType type,
            @RequestParam(required = false) AppStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false, defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        // 종료일 포함 보정
        if (end != null) end = end.withSecond(59).withNano(999_999_999);

        Sort sort = Sort.by("createdAt");
        sort = "asc".equalsIgnoreCase(dir) ? sort.ascending() : sort.descending();


        var pageable = PageRequest.of(page, size, sort);
        var result = appRepo.search(q, type, status, start, end, pageable);

        // ✅ 총 신청 인원(= totalCount 합계)
        long totalApplicants = appRepo.sumTotalCount(q, type, status, start, end);

        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("dir", dir);
        model.addAttribute("totalApplicants", totalApplicants);

        model.addAttribute("types", DomainType.values());
        model.addAttribute("statuses", AppStatus.values());
        return "admin";
    }

    @GetMapping("/applications/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        var app = appRepo.findById(id).orElseThrow();
        var members = memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(id);
        long ticketCount = ticketRepo.countByApplication_ApplicationId(id);

        model.addAttribute("app", app);
        model.addAttribute("members", members);
        model.addAttribute("ticketCount", ticketCount);
        return "admin_detail";
    }

    @PostMapping("/applications/{id}/cancel")
    public String cancel(@PathVariable UUID id, @RequestHeader("referer") Optional<String> back) {
        service.cancelByApplicationId(id);
        return "redirect:" + back.orElse("/admin");
    }

    @GetMapping("/export/xlsx")
    public void exportXlsx(HttpServletResponse res) throws java.io.IOException {
        var apps = appRepo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            // 시트 1: applications
            var sh = wb.createSheet("applications");
            int r = 0;
            var head = sh.createRow(r++);
            String[] h1 = {"생성시각","ID","유형","연주자","상태","인원","대표수령","대표명","대표이메일","대표전화"};
            for (int c=0;c<h1.length;c++) head.createCell(c).setCellValue(h1[c]);

            for (var a : apps) {
                var row = sh.createRow(r++);
                int c=0;
                row.createCell(c++).setCellValue(a.getCreatedAt()==null? "" : a.getCreatedAt().toString());
                row.createCell(c++).setCellValue(a.getApplicationId()==null? "" : a.getApplicationId().toString());
                row.createCell(c++).setCellValue(a.getDomainType()==null? "" : a.getDomainType().ko());
                row.createCell(c++).setCellValue(a.getPerformer()==null? "" : a.getPerformer().getName());
                row.createCell(c++).setCellValue(a.getStatus()==null? "" : a.getStatus().ko());
                row.createCell(c++).setCellValue(a.getTotalCount());
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(a.isRepDelivery())? "Y":"N");
                row.createCell(c++).setCellValue(a.getRepName()==null? "" : a.getRepName());
                row.createCell(c++).setCellValue(a.getRepEmail()==null? "" : a.getRepEmail());
                row.createCell(c++).setCellValue(a.getRepPhone()==null? "" : a.getRepPhone());
            }
            for (int c=0;c<h1.length;c++) sh.autoSizeColumn(c);

            // 시트 2: members
            var sh2 = wb.createSheet("members");
            int r2 = 0;
            var head2 = sh2.createRow(r2++);
            String[] h2 = {"응모ID","행","이름","이메일","전화"};
            for (int c=0;c<h2.length;c++) head2.createCell(c).setCellValue(h2[c]);

            // 멤버는 한 번에 조회(간단하게 each 내부에서 repo 호출해도 되지만, 필요시 서비스에서 배치로 꺼내도 OK)
            for (var a : apps) {
                var members = memberRepo.findByApplication_ApplicationIdOrderByRowOrderAsc(a.getApplicationId());
                for (var m : members) {
                    var row = sh2.createRow(r2++);
                    int c=0;
                    row.createCell(c++).setCellValue(a.getApplicationId().toString());
                    row.createCell(c++).setCellValue(m.getRowOrder());
                    row.createCell(c++).setCellValue(m.getName()==null? "" : m.getName());
                    row.createCell(c++).setCellValue(m.getEmail()==null? "" : m.getEmail());
                    row.createCell(c++).setCellValue(m.getPhone()==null? "" : m.getPhone());
                }
            }
            for (int c=0;c<h2.length;c++) sh2.autoSizeColumn(c);

            // 응답 헤더
            res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            var filename = java.net.URLEncoder.encode("libera_applications.xlsx", java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");
            res.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"+filename);
            wb.write(res.getOutputStream());
        }
    }

    @PostMapping("/applications/{id}/issue-tickets")
    public String issue(@PathVariable UUID id){
        ticketIssue.issueForApplicationId(id);
        return "redirect:/admin/applications/" + id;
    }

    @GetMapping("/statistic")
    public String statistic(Model model) {

        // 전체 응모 현황
        long totalApps = appRepo.count();
        long canceledApps = appRepo.countByStatus(AppStatus.CANCELED);
        long submittedApps = appRepo.countByStatus(AppStatus.SUBMITTED);

        // 총 인원 수
        Integer totalPeople = appRepo.sumTotalCount();

        // 연주자별 인원 수
        var performerStats = appRepo.findPerformerStats();

        model.addAttribute("totalApps", totalApps);
        model.addAttribute("canceledApps", canceledApps);
        model.addAttribute("submittedApps", submittedApps);
        model.addAttribute("totalPeople", totalPeople);
        model.addAttribute("performerStats", performerStats);

        return "admin_statistic";
    }
}
