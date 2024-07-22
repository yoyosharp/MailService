package com.app.MailService.RabbitMQ;

import com.app.MailService.Entity.EmailTemplate;
import com.app.MailService.Entity.QueueMessage;
import com.app.MailService.Repository.EmailTemplateRepository;
import com.app.MailService.Repository.QueueMessageRepository;
import com.app.MailService.Service.GmailService;
import com.app.MailService.Service.ZeptoMailService;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class Consumer {

    private final QueueMessageRepository queueMessageRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final ZeptoMailService zeptoMailService;
    private final GmailService gmailService;
    @Value("${application.defaultMailClient}")
    private String defaultProvider;

    @Autowired
    public Consumer(QueueMessageRepository queueMessageRepository,
                    EmailTemplateRepository emailTemplateRepository,
                    GmailService gmailService,
                    ZeptoMailService zeptoMailService
    ) {
        this.queueMessageRepository = queueMessageRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.gmailService = gmailService;
        this.zeptoMailService = zeptoMailService;
    }

    @RabbitListener(queues = Constants.REGISTER_OTP_QUEUE)
    public void receiveRegisterMessage(String message) {
        handle(message, Constants.REGISTER_OTP_QUEUE);
    }

    @RabbitListener(queues = Constants.LOGIN_OTP_QUEUE)
    public void receiveLoginMessage(String message) {
        handle(message, Constants.LOGIN_OTP_QUEUE);
    }

    @RabbitListener(queues = Constants.FORGOT_PASSWORD_OTP_QUEUE)
    public void receiveForgotPasswordMessage(String message) {
        handle(message, Constants.FORGOT_PASSWORD_OTP_QUEUE);
    }

    private void handle(String message, String queueName) {
        log.info("Received message from {}: {}", queueName, message);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            QueueMessage queueMessage = objectMapper.readValue(message, QueueMessage.class);
            Map<String, String> data = objectMapper.readValue(queueMessage.getData(), new TypeReference<>() {
            });

            String htmlBody = createHtmlBody(queueMessage.getEmailTemplate(), data);

            boolean result = sendMessage(queueMessage, htmlBody);
            if (result) {
                queueMessage.setEmailSent(true);
                queueMessageRepository.save(queueMessage);
            }
        } catch (Exception e) {
            log.error("Error while processing message: {}", e.getMessage());
        }
    }

    private String createHtmlBody(String templateName, Map<String, String> data) {
        EmailTemplate emailTemplate = emailTemplateRepository.findByName(templateName);
        if (emailTemplate == null) {
            throw new RuntimeException("Email template not found, requested template: " + templateName);
        }
        if (!emailTemplate.isActive()) {
            throw new RuntimeException("Template " + templateName + " is not active");
        }
        return emailTemplate.fillTemplate(data);
    }

    private boolean sendMessage(QueueMessage queueMessage, String htmlBody) {
        return switch (defaultProvider) {
            case Constants.ZEPTO_MAIL ->
                    zeptoMailService.sendSingleMailByZeptoMail(queueMessage.getFromAddress(), queueMessage.getSenderName(), queueMessage.getToAddress(), queueMessage.getSubject(), htmlBody);
            case Constants.GMAIL_API ->
                    gmailService.sendMailByGmailApi(queueMessage.getToAddress(), queueMessage.getSubject(), htmlBody);
            case Constants.GMAIL_BY_JAVA_MAILER ->
                    gmailService.sendSingleMailByJavaMailer(queueMessage.getToAddress(), queueMessage.getSubject(), htmlBody);
            default -> false;
        };
    }
}