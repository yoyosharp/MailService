package com.app.MailService.RabbitMQ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Publisher {

    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange topicExchange;

    @Autowired
    public Publisher(RabbitTemplate rabbitTemplate, TopicExchange topicExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.topicExchange = topicExchange;
    }

    public void sendMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend(topicExchange.getName(), routingKey, message);
        logger.info("Successfully enqueued message with routing key {}: {}", routingKey, message);
    }
}
