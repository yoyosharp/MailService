package com.app.MailService.Entity;

import com.app.MailService.Model.DTO.OtpData;
import com.app.MailService.Model.DTO.RequestContext;
import com.app.MailService.Utilities.Constants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;

@Entity
@Table(name = "otp")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "client")
    private String clientId;

    @Column(name = "otp_type")
    private String type;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "expiring_time")
    private Timestamp expiringTime;

    @Column(name = "max_retry")
    private Integer maxRetry;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_resend")
    private Integer maxResend;

    @Column(name = "resend_count")
    private Integer resendCount;

    @Column(name = "send_info")
    private String sendInfo;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public Otp(OtpData data, int maxRetry, int maxResend) {
        this();
        this.setClientId(RequestContext.get("clientId"));
        this.setType(data.getOtpType());
        this.setSendInfo(data.getSendInfo());
        this.setTrackingId(RequestContext.get("trackingId"));
        this.setStatus(Constants.OTP_STATUS_PENDING);
        this.setMaxRetry(maxRetry);
        this.setMaxResend(maxResend);
        this.setRetryCount(0);
        this.setResendCount(0);
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}
