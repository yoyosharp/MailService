package com.app.MailService.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "mailServiceExchange";
    public static final String DEMO_QUEUE = "demoQueue";
    public static final String ANOTHER_QUEUE = "anotherQueue";
    public static final String DEMO_ROUTING_KEY = "demo";
    public static final String ANOTHER_DEMO_ROUTING_KEY = "anotherDemo";
    public static final List<String> routingKeys = List.of(DEMO_ROUTING_KEY, ANOTHER_DEMO_ROUTING_KEY);

    /**
     * Create a topic exchange
     *
     * @return a TopicExchange instance with the name "mailServiceExchange"
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Create queues
     *
     * @return Queue instances with the name "demoQueue" and "anotherQueue"
     */
    @Bean
    public Queue demoQueue() {
        return new Queue(DEMO_QUEUE);
    }

    @Bean
    public Queue anotherQueue() {
        return new Queue(ANOTHER_QUEUE);
    }

    /**
     * Define bindings
     *
     * @return Binding instances with routing keys "demo" and "anotherDemo"
     */
    @Bean
    public Binding bindingDemoQueue(Queue demoQueue, TopicExchange mailServiceExchange) {
        return BindingBuilder.bind(demoQueue).to(mailServiceExchange).with(DEMO_ROUTING_KEY);
    }

    @Bean
    public Binding bindingAnotherQueue(Queue anotherQueue, TopicExchange mailServiceExchange) {
        return BindingBuilder.bind(anotherQueue).to(mailServiceExchange).with(ANOTHER_DEMO_ROUTING_KEY);
    }

}