package com.app.MailService.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ZeptoMailService {

    @Value("${zeptoMail.url}")
    private String zeptoMailUrl;
    @Value("${zeptoMail.token}")
    private String zeptoMailToken;


    public boolean sendSingleMailByZeptoMail(String senderEmail, String senderName, String recipientAddress, String subject, String htmlBody) {
        From from = new From(senderEmail, senderName);
        List<To> to = new ArrayList<>();
        to.add(new To(new EmailAddress(recipientAddress)));

        ZeptoMailRequest request = new ZeptoMailRequest(from, to, htmlBody, subject);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", zeptoMailToken);

        try {
            HttpEntity<ZeptoMailRequest> entity = new HttpEntity<>(request, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<?> response = restTemplate.exchange(zeptoMailUrl, HttpMethod.POST, entity, Object.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully by ZeptoMail, recipient: {}", recipientAddress);
                return true;
            } else {
                log.error("The mail service returned an error {}", response.getBody());
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
