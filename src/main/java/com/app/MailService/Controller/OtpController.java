package com.app.MailService.Controller;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Model.DTO.OtpResponseDTO;
import com.app.MailService.Model.DTO.UserCardInfo;
import com.app.MailService.Model.Request.GenerateOtpRequest;
import com.app.MailService.Model.Request.VerifyOtpRequest;
import com.app.MailService.Model.Response.ApiResponse;
import com.app.MailService.Service.OtpCardService;
import com.app.MailService.Service.OtpService;
import com.app.MailService.Utilities.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;

import static com.app.MailService.Utilities.EndPointsConstants.*;

@Controller
@RequestMapping(API_V1_OTP)
public class OtpController {

    private final OtpService otpService;
    private final OtpCardService otpCardService;

    @Autowired
    public OtpController(OtpService otpService, OtpCardService otpCardService) {
        this.otpService = otpService;
        this.otpCardService = otpCardService;
    }

    @PostMapping(GENERATE_OTP)
    public ResponseEntity<?> generateOtp(@RequestBody GenerateOtpRequest request) {
        Otp otp = otpService.createOtp(request);
        return generateOtpResponse(otp, "OTP generated successfully");
    }

    @PostMapping(VERIFY_OTP)
    public ResponseEntity<?> validateOtp(@RequestBody VerifyOtpRequest request) {
        Otp otp = otpService.verifyOtp(request);
        String message = "OTP verifying result: ";
        message += otp.getStatus().equals(Constants.OTP_STATUS_VERIFIED) ? "successful" : "failed";
        return generateOtpResponse(otp, message);
    }

    private ResponseEntity<ApiResponse> generateOtpResponse(Otp otp, String message) {
        OtpResponseDTO otpResponseDTO = new OtpResponseDTO(otp);
        ApiResponse response = new ApiResponse();
        response.setStatus(HttpStatus.OK.value());
        response.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
        response.setMessage(message);
        response.setResult(otpResponseDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(RESEND_OTP)
    public ResponseEntity<?> resendOtp(@RequestParam String trackingId) {
        Otp otp = otpService.resendOtp(trackingId);
        return generateOtpResponse(otp, "OTP resent successfully");
    }

    @PostMapping(CREATE_CARD)
    public ResponseEntity<?> userCreateCard(@RequestBody UserCardInfo userCardInfo) {
        boolean result = otpCardService.userCreateOtpCard(userCardInfo);
        ApiResponse response = new ApiResponse();
        response.setStatus(HttpStatus.OK.value());
        response.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
        response.setMessage("Card created successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
