package com.app.MailService.Repository;

import com.app.MailService.Entity.OtpCardToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtpCardTokenRepository extends JpaRepository<OtpCardToken, Long> {
    List<OtpCardToken> findByCardSerial(String cardSerial);

    OtpCardToken findByCardSerialAndPosition(String cardSerial, Integer position);
}