package com.app.MailService.Repository;

import com.app.MailService.Entity.OtpCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCardRepository extends JpaRepository<OtpCard, Long> {
    @Query("SELECT o FROM OtpCard o LEFT JOIN FETCH o.otpCardTokens WHERE o.userId = :userId AND o.status = :status")
    OtpCard getUserOtpCard(@Param("userId") Long userId, @Param("status") String status);
}