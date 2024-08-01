package com.app.MailService.Entity;

import com.app.MailService.Model.Projection.OtpCardProjection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "otp_cards")
@Getter
@Setter
@NoArgsConstructor
public class OtpCard implements OtpCardProjection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "card_serial")
    private String cardSerial;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "issue_at")
    private Timestamp issueAt;

    @Column(name = "expire_at")
    private Timestamp expireAt;

    @Column(name = "status")
    private String status;

    @Column(name = "hash_token")
    private String hashToken;

    @OneToMany(mappedBy = "otpCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OtpCardToken> otpCardTokens;
}
