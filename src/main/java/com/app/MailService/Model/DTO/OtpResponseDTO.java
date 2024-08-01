package com.app.MailService.Model.DTO;

import com.app.MailService.Entity.Otp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;

@Getter
@NoArgsConstructor
public class OtpResponseDTO {
    private String trackingId;
    private String type;
    private Timestamp expiringTime;
    private Integer maxRetry;
    private Integer retryCount;
    private Integer maxResend;
    private Integer resendCount;
    private Map<String, String> sendInfo;
    private String status;
    private Timestamp createdAt;

    public OtpResponseDTO(Otp otp) {
        this();
        this.trackingId = otp.getTrackingId();
        this.type = otp.getType();
        this.expiringTime = otp.getExpiringTime();
        this.maxRetry = otp.getMaxRetry();
        this.retryCount = otp.getRetryCount();
        this.maxResend = otp.getMaxResend();
        this.resendCount = otp.getResendCount();
        try {
            this.sendInfo = new ObjectMapper().readValue(otp.getSendInfo(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse sendInfo", e);
        }
        this.status = otp.getStatus();
        this.createdAt = otp.getCreatedAt();
    }
}
