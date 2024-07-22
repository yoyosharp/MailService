package com.app.MailService.Controller;

import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Model.Response.ApiErrorResponse;
import com.app.MailService.Model.Response.ApiResponse;
import com.app.MailService.Service.SendMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;

import static com.app.MailService.Utilities.EndPointsConstants.API_V1_SEND_MAIL;
import static com.app.MailService.Utilities.EndPointsConstants.SEND_MAIL_SINGLE;

@RestController
@RequestMapping(API_V1_SEND_MAIL)
public class SendMailController {

    private final SendMailService sendMailService;
    Logger logger = LoggerFactory.getLogger(SendMailController.class);

    @Autowired
    public SendMailController(SendMailService sendMailService) {
        this.sendMailService = sendMailService;
    }

    @PostMapping(SEND_MAIL_SINGLE)
    public ResponseEntity<?> publishMessage(@RequestBody EmailMessageRequest request) {

        try {
            ApiResponse apiResponse = new ApiResponse();
            String trackingId = sendMailService.enQueue(request);
            apiResponse.setMessage("Message sent successfully!");
            apiResponse.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
            apiResponse.setStatus(HttpStatus.OK.value());
            apiResponse.setResult(trackingId);
            return new ResponseEntity<>(apiResponse, HttpStatus.OK);
        } catch (Exception e) {
            ApiErrorResponse response = new ApiErrorResponse();
            response.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Internal server error");
            response.setPath("/api/send-mail/send-single-mail");
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
