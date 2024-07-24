package com.app.MailService.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "otp_card_tokens")
@Getter
@Setter
@NoArgsConstructor
public class OtpCardToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_serial")
    private String cardSerial;

    @Column(name = "position")
    private Integer position;

    @Column(name = "token")
    private String token;

    @Column(name = "hash_token")
    private String hashToken;

    @Column(name = "create_at")
    private Timestamp createAt;

    public OtpCardToken(String cardSerial, Integer position, String token) {
        this.cardSerial = cardSerial;
        this.position = position;
        this.token = token;
        this.createAt = new Timestamp(System.currentTimeMillis());
    }
}
