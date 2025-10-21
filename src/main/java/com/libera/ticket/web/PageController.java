package com.libera.ticket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/about")
    public String about(Model model) {
        // 필요하면 이미지 경로를 동적으로 주입
         model.addAttribute("aboutImg1", "/img/Libera_image1.jpg");
         model.addAttribute("aboutImg2", "/img/Libera_image1.jpg");
        return "about"; // src/main/resources/templates/about.html
    }
}
