package com.app.MailService.Service;

import com.app.MailService.Entity.QueueMessage;
import com.app.MailService.Model.DTO.SendMailDTO;
import com.app.MailService.Model.Request.EmailMessageRequest;
import com.app.MailService.RabbitMQ.Publisher;
import com.app.MailService.Repository.QueueMessageRepository;
import com.app.MailService.Utilities.AESHelper;
import com.app.MailService.Utilities.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;

@Service
@Slf4j
public class SendMailService {

    private final Publisher publisher;
    private final QueueMessageRepository queueMessageRepository;
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
        String routingKey = request.getRequestType();
        if (!Constants.routingKeys.contains(routingKey)) {
            throw new RuntimeException("Invalid request type");
        }
        try {
            String content = request.isEncrypted() ?
                    AESHelper.decrypt(request.getContent(), aesKey, aesIv) : request.getContent();

            log.info("Enqueuing message: {}", content);

            ObjectMapper objectMapper = new ObjectMapper();
            SendMailDTO sendMailData = objectMapper.readValue(content, new TypeReference<>() {
            });

            QueueMessage queueMessage = generateQueueMessage(sendMailData);
            queueMessageRepository.save(queueMessage);

            String strMessage = objectMapper.writeValueAsString(queueMessage);
            publisher.sendMessage(routingKey, strMessage);

            return queueMessage.getTrackingId();
        } catch (Exception e) {
            log.error("Error while enqueueing message: {}", e.getMessage());
            throw new RuntimeException("Error while processing message");
        }
    }

    private QueueMessage generateQueueMessage(SendMailDTO sendMailData) throws JsonProcessingException {
        QueueMessage queueMessage = new QueueMessage();
        try {
            queueMessage.setTrackingId((String) RequestContextHolder.getRequestAttributes().getAttribute("trackingId", RequestAttributes.SCOPE_REQUEST));
            queueMessage.setClientId((String) RequestContextHolder.getRequestAttributes().getAttribute("clientId", RequestAttributes.SCOPE_REQUEST));
        } catch (NullPointerException e) {
            queueMessage.setTrackingId(UUID.randomUUID().toString());
            queueMessage.setClientId(null);
        }

        queueMessage.setFromAddress(sendMailData.getFromAddress());
        queueMessage.setSenderName(sendMailData.getSenderName());
        queueMessage.setToAddress(sendMailData.getToAddress());
        queueMessage.setSubject(sendMailData.getSubject());
        queueMessage.setEmailTemplate(sendMailData.getEmailTemplate());
        queueMessage.setData(new ObjectMapper().writeValueAsString(sendMailData.getData()));
        queueMessage.setMessageSent(false);
        return queueMessage;
    }
}
