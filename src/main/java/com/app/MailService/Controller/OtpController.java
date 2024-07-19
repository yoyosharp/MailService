package com.app.MailService.Controller;

import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class OtpController {

    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generete")
    public ResponseEntity<?> generateOtp(@RequestBody GenerateOtpRequest request) {

        String otpTrackingId = otpService.createOtp(request);

        return ResponseEntity.ok("Success");
    }
}
