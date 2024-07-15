package com.app.MailService.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OtpController {
    @GetMapping("/demo")
    public String demo() {
        return "Success";
    }
}
