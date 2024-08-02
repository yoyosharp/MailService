package com.app.MailService.Model.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Data
public class GenerateOtpDTO {
    private String otpType;
    private Map<String, String> sendInfo;
}
