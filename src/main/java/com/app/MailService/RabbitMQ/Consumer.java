package com.app.MailService.RabbitMQ;

import com.app.MailService.Entity.EmailTemplate;
import com.app.MailService.Entity.QueueMessage;
import com.app.MailService.Repository.EmailTemplateRepository;
import com.app.MailService.Repository.QueueMessageRepository;
import com.app.MailService.Utilities.SendByZeptoMail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class Consumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private final QueueMessageRepository queueMessageRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    @Value("${zeptoMail.url}")
    private String zeptoMailUrl;
    @Value("${zeptoMail.token}")
    private String zeptoMailToken;

    @Autowired
    public Consumer(QueueMessageRepository queueMessageRepository,
                    EmailTemplateRepository emailTemplateRepository
    ) {
        this.queueMessageRepository = queueMessageRepository;
        this.emailTemplateRepository = emailTemplateRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.DEMO_QUEUE)
    public void receiveDemoMessage(String message) {
        handle(message, RabbitMQConfig.DEMO_QUEUE);
    }

    @RabbitListener(queues = RabbitMQConfig.ANOTHER_QUEUE)
    public void receiveAnotherDemoQueueMessage(String message) {
        handle(message, RabbitMQConfig.ANOTHER_QUEUE);
    }

    private void handle(String message, String queueName) {
        logger.info("Received message from {}: {}", queueName, message);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            QueueMessage queueMessage = objectMapper.readValue(message, QueueMessage.class);
            Map<String, String> data = objectMapper.readValue(queueMessage.getData(), new TypeReference<>() {
            });

            EmailTemplate emailTemplate = emailTemplateRepository.findByName(queueMessage.getEmailTemplate());
            if (emailTemplate == null) {
                throw new RuntimeException("Email template not found");
            }
            if (!emailTemplate.isActive()) {
                throw new RuntimeException("Email template is not active");
            }
            String htmlBody = emailTemplate.fillTemplate(data);

            boolean result = SendByZeptoMail.singleMailByZeptoMail(zeptoMailUrl, zeptoMailToken, queueMessage.getFromAddress(), queueMessage.getSenderName(), queueMessage.getToAddress(), queueMessage.getSubject(), htmlBody);

            if (result) {
                queueMessage.setEmailSent(true);
            }
            queueMessageRepository.save(queueMessage);

        } catch (Exception e) {
            logger.error("Error while processing message: {}", e.getMessage());
        }
    }
}