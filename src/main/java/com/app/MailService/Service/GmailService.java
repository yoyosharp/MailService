package com.app.MailService.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class GmailService {
    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
    @Autowired
    private JavaMailSender mailSender;

    //simply use javaMailSender
    public boolean sendSingleMailByGmail(String toAddress, String subject, String htmlBody) {
        try {
            MimeMailMessage message = new MimeMailMessage(mailSender.createMimeMessage());
            MimeMessageHelper helper = new MimeMessageHelper(message.getMimeMessage(), true);

            helper.setFrom("noreply@demo.com", "System Notification");
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message.getMimeMessage());
            return true;
        } catch (Exception e) {
            logger.error("Error while sending email: {}", e.getMessage());
            return false;
        }
    }

    
}
