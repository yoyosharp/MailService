package com.app.MailService.Model.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMailDTO {

    private String fromAddress;
    private String senderName;
    private String toAddress;
    private String subject;
    private String emailTemplate;
    private Map<String, String> data;
}
