package com.app.MailService.Repository;

import com.app.MailService.Entity.OtpCard;
import com.app.MailService.Entity.Projection.OtpCardProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpCardRepository extends JpaRepository<OtpCard, Long> {
    @Query("SELECT o FROM OtpCard o LEFT JOIN FETCH o.otpCardTokens WHERE o.userId = :userId AND o.status = :status")
    Optional<OtpCard> getUserOtpCard(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT o FROM OtpCard o LEFT JOIN FETCH o.otpCardTokens WHERE o.cardSerial = :cardSerial AND o.status = :status")
    Optional<OtpCard> findByCardSerial(@Param("cardSerial") String cardSerial, @Param("status") String status);

    boolean existsByUserIdAndStatus(Long userId, String status);

    @Query("SELECT o FROM OtpCard o")
    Page<OtpCardProjection> fetchAll(Pageable page);

    Page<OtpCardProjection> findAllByUserIdAndStatus(Long userId, String status, Pageable page);

    Page<OtpCardProjection> findAllByUserId(Long userId, Pageable page);

    Page<OtpCardProjection> findAllByStatus(String status, Pageable pageable);
}