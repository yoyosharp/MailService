package com.app.MailService.RabbitMQ;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Publisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange topicExchange;

    @Autowired
    public Publisher(RabbitTemplate rabbitTemplate, TopicExchange topicExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.topicExchange = topicExchange;
    }

    public void sendMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend(topicExchange.getName(), routingKey, message);
        log.info("Successfully enqueued message with routing key {}: {}", routingKey, message);
    }
}
