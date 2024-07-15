package com.app.MailService.Utilities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class SendByZeptoMail {

    private static final Logger log = LoggerFactory.getLogger(SendByZeptoMail.class);

    public static boolean singleMailByZeptoMail(String zeptoMailUrl, String zeptoMailToken, String senderEmail, String senderName, String recipientAddress, String subject, String htmlBody) {
        From from = new From(senderEmail, senderName);
        List<To> to = new ArrayList<>();
        to.add(new To(new EmailAddress(recipientAddress)));

        RestTemplate restTemplate = new RestTemplate();
        ZeptoMailRequest request = new ZeptoMailRequest(from, to, htmlBody, subject);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", zeptoMailToken);

        try {
            HttpEntity<ZeptoMailRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(zeptoMailUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            } else {
                log.error("Error while sending email: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Error while sending email: {}", e.getMessage());
            return false;
        }
    }

    @AllArgsConstructor
    @Getter
    private static class From {
        @JsonProperty("address")
        private String email;
        @JsonProperty("name")
        private String name;
    }

    @AllArgsConstructor
    @Getter
    private static class To {
        @JsonProperty("email_address")
        private EmailAddress emailAddress;
    }

    @AllArgsConstructor
    @Getter
    private static class EmailAddress {
        @JsonProperty("address")
        private String address;
    }

    @AllArgsConstructor
    @Getter
    private static class ZeptoMailRequest {
        @JsonProperty("from")
        private From from;
        @JsonProperty("to")
        private List<To> to;
        @JsonProperty("htmlbody")
        private String htmlBody;
        @JsonProperty("subject")
        private String subject;
    }

}
