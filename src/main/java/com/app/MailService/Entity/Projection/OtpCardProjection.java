package com.app.MailService.Entity.Projection;

import java.sql.Timestamp;

/**
 * Projection for {@link com.app.MailService.Entity.OtpCard}
 */
public interface OtpCardProjection {
    Long getId();

    Long getUserId();

    String getCardSerial();

    Timestamp getPublishedAt();

    Timestamp getIssueAt();

    Timestamp getExpireAt();

    String getStatus();
}