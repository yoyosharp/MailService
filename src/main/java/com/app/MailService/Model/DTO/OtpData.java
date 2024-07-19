package com.app.MailService.Model.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class OtpData {
    @JsonProperty("clientId")
    private String clientId;
    @JsonProperty("otpType")
    private String otpType;
    @JsonProperty("sendInfo")
    private SendInfo sendInfo;

    @AllArgsConstructor
    @Getter
    @Setter
    public static class SendInfo {
        @JsonProperty("sendType")
        private String sendType;
        @JsonProperty("target")
        private String target;
    }


}
