package com.app.MailService.Controller;

import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.Response.ApiResponse;
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

@RestController
@RequestMapping("/api/send-mail")
public class SendMailController {

    private final SendMailService sendMailService;
    Logger logger = LoggerFactory.getLogger(SendMailController.class);

    @Autowired
    public SendMailController(SendMailService sendMailService) {
        this.sendMailService = sendMailService;
    }

    @RequestMapping("/demo")
    public ResponseEntity<?> demo() {
        return new ResponseEntity<>("Success", HttpStatus.OK);

    }

    @PostMapping("/send-single-mail")
    public ResponseEntity<?> publishMessage(@RequestBody EmailMessageRequest request) {
        ApiResponse apiResponse = new ApiResponse();
        try {
            String trackingId = sendMailService.enQueue(request);
            apiResponse.setTrackingId(trackingId);
            apiResponse.setUrl("/api/send-mail/send-single-mail");
            apiResponse.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
            apiResponse.setStatus("Success");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
