package com.app.MailService.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/hello")
    public ResponseEntity<?> demo() {
        return new ResponseEntity<>("Hello World", HttpStatus.OK);
    }

}
