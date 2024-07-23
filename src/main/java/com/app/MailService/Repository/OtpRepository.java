package com.app.MailService.Repository;

import com.app.MailService.Entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Otp findByTrackingId(String trackingId);
}
