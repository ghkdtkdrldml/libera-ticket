package com.libera.ticket.web;

import com.libera.ticket.repo.ApplicationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class PageController {

    private final ApplicationRepo appRepo;

    @GetMapping("/about")
    public String about(Model model) {
        // 필요하면 이미지 경로를 동적으로 주입
         model.addAttribute("aboutImg1", "/img/Libera_image1.jpg");
         model.addAttribute("aboutImg2", "/img/Libera_image2.jpg");
        return "about"; // src/main/resources/templates/about.html
    }

    @GetMapping("/libera/ticket")
    public String liberaTicket(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.ui.Model model
    ) {
        // '허영우'는 검색 불가 → 검색어 무시
        if ("허영우".equals(q)) q = null;
        if (q != null) {
            q = q.trim();
            if (q.isBlank() || "null".equalsIgnoreCase(q)) q = null; // 안전빵
        }

        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        var result = appRepo.findSubmittedByPerformerLike(q, pageable);
        long totalApps = appRepo.countSubmittedFiltered(q);            // 취소 제외
        int totalPeople = appRepo.sumPeopleSubmittedFiltered(q);       // 취소 제외

        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("totalApps", totalApps);
        model.addAttribute("totalPeople", totalPeople);
        return "libera_ticket";
    }

}
