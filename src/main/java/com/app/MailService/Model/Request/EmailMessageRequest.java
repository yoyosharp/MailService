package com.app.MailService.Model.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailMessageRequest {

    private String requestType;
    private String content;
    private boolean isEncrypted;
}
