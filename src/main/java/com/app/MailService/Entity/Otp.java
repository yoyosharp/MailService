package com.app.MailService.Entity;

import com.app.MailService.Utilities.Constants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "otp")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
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

    public Otp(long expirationTimeInSecond, int maxRetry, int maxResend) {
        this();
        this.setTrackingId(UUID.randomUUID().toString());
        this.setStatus(Constants.OTP_STATUS_PENDING);
        this.setMaxRetry(maxRetry);
        this.setMaxResend(maxResend);
        this.setRetryCount(0);
        this.setResendCount(0);

        this.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        this.setExpiringTime(new Timestamp(System.currentTimeMillis() + (expirationTimeInSecond * 1000)));

    }
}
