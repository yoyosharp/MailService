package com.app.MailService.Model.DTO;

import com.app.MailService.Entity.Otp;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

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
    private String sendInfo;
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
        this.sendInfo = otp.getSendInfo();
        this.status = otp.getStatus();
        this.createdAt = otp.getCreatedAt();
    }
}
