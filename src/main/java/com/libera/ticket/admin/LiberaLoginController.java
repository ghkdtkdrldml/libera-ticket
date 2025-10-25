package com.libera.ticket.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LiberaLoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "libera_login"; // templates/libera_login.html
    }
}
