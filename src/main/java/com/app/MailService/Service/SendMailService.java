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
    private final String aesKey;
    private final String aesIv;
    private final QueueMessageRepository queueMessageRepository;

    Logger logger = LoggerFactory.getLogger(SendMailController.class);

    @Autowired
    public SendMailService(Publisher publisher,
                           @Value("${aes.key}") String aesKey,
                           @Value("${aes.iv}") String aesIv,
                           QueueMessageRepository queueMessageRepository) {
        this.queueMessageRepository = queueMessageRepository;
        this.publisher = publisher;
        this.aesKey = aesKey;
        this.aesIv = aesIv;
    }

    @Transactional
    public void enQueue(EmailMessageRequest request) {
        String routingKey = request.getRequestType();
        String content = request.getContent();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (!RabbitMQConfig.routingKeys.contains(routingKey)) {
                throw new RuntimeException("Invalid routing key");
            }

            if (request.isEncrypted()) {
                content = AESHelper.decrypt(content, aesKey, aesIv);
            }

            Map<String, String> sendMailData = objectMapper.readValue(content, new TypeReference<>() {
            });
            QueueMessage queueMessage = new QueueMessage(sendMailData);
            queueMessageRepository.save(queueMessage);

            String strMessage = objectMapper.writeValueAsString(queueMessage);
            publisher.sendMessage(routingKey, strMessage);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error while enqueueing message: {}", e.getMessage());
            throw new RuntimeException("Error while processing message");
        }
    }
}
