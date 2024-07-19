package com.app.MailService.Controller;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Model.Response.ApiResponse;
import com.app.MailService.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;

@Controller
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateOtp(@RequestBody GenerateOtpRequest request) {

        Otp otp = otpService.createOtp(request);

        ApiResponse response = new ApiResponse();
        response.setStatus(HttpStatus.OK.value());
        response.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
        response.setMessage("OTP generated successfully");
        response.setResult(otp);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
