package com.app.MailService.Entity;

import com.app.MailService.Model.DTO.RequestContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.Map;

@Entity
@Table(name = "queue_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QueueMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "client")
    private String clientId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "subject")
    private String subject;

    @Column(name = "email_template")
    private String emailTemplate;

    @Column(name = "data")
    private String data;

    @Column(name = "email_sent")
    private boolean emailSent;

    @CreatedDate
    @Column(name = "created_at")
    private Timestamp createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public QueueMessage(Map<String, String> data) {
        this.trackingId = RequestContext.get("trackingId");
        this.clientId = RequestContext.get("clientId");
        this.fromAddress = data.get("fromAddress");
        this.senderName = data.get("senderName");
        this.toAddress = data.get("toAddress");
        this.subject = data.get("subject");
        this.emailTemplate = data.get("emailTemplate");
        this.data = data.get("data");
        this.emailSent = false;
    }
}
