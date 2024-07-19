package com.app.MailService.Service;

import com.app.MailService.Controller.SendMailController;
import com.app.MailService.Entity.QueueMessage;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.RabbitMQ.Publisher;
import com.app.MailService.RabbitMQ.RabbitMQConfig;
import com.app.MailService.Repository.QueueMessageRepository;
import com.app.MailService.Utilities.AESHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SendMailService {

    private final Publisher publisher;
    private final QueueMessageRepository queueMessageRepository;
    Logger logger = LoggerFactory.getLogger(SendMailController.class);
    @Value("${aes.key}")
    private String aesKey;
    @Value("${aes.iv}")
    private String aesIv;

    @Autowired
    public SendMailService(Publisher publisher,
                           QueueMessageRepository queueMessageRepository) {
        this.queueMessageRepository = queueMessageRepository;
        this.publisher = publisher;
    }

    @Transactional
    public String enQueue(EmailMessageRequest request) {
        System.out.println(request.isEncrypted());
        String routingKey = request.getRequestType();
        String content = request.getContent();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!RabbitMQConfig.routingKeys.contains(routingKey)) {
            throw new RuntimeException("Invalid routing key");
        }
        try {
            if (request.isEncrypted()) {
                content = AESHelper.decrypt(content, aesKey, aesIv);
            }
            Map<String, String> sendMailData = objectMapper.readValue(content, new TypeReference<>() {
            });
            QueueMessage queueMessage = new QueueMessage(sendMailData);
            queueMessageRepository.save(queueMessage);
            String strMessage = objectMapper.writeValueAsString(queueMessage);
            publisher.sendMessage(routingKey, strMessage);
            return queueMessage.getTrackingId();
        } catch (Exception e) {
            logger.error("Error while enqueueing message: {}", e.getMessage());
            throw new RuntimeException("Error while processing message");
        }
    }
}
