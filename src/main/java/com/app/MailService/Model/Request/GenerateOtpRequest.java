package com.app.MailService.Model.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GenerateOtpRequest {
    private String content;
}
