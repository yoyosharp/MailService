package com.app.MailService.Model.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpDTO {
    private String otpCode;
    private String trackingId;
}
