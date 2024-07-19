package com.app.MailService.RabbitMQ;

import com.app.MailService.Utilities.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /**
     * Create a topic exchange
     *
     * @return a TopicExchange instance with the name "mailServiceExchange"
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(Constants.EXCHANGE_NAME);
    }

    /**
     * Create queues
     *
     * @return Queue instances with the name "demoQueue" and "anotherQueue"
     */
    @Bean
    public Queue registerOtpQueue() {
        return new Queue(Constants.REGISTER_OTP_QUEUE);
    }

    @Bean
    public Queue loginOtpQueue() {
        return new Queue(Constants.LOGIN_OTP_QUEUE);
    }

    @Bean
    public Queue forgotPasswordOtpQueue() {
        return new Queue(Constants.FORGOT_PASSWORD_OTP_QUEUE);
    }

    /**
     * Define bindings
     *
     * @return Binding instances with routing keys "demo" and "anotherDemo"
     */
    @Bean
    public Binding bindingRegisterQueue(Queue registerOtpQueue, TopicExchange mailServiceExchange) {
        return BindingBuilder.bind(registerOtpQueue).to(mailServiceExchange).with(Constants.REGISTER_OTP_ROUTING_KEY);
    }

    @Bean
    public Binding bindingLoginQueue(Queue loginOtpQueue, TopicExchange mailServiceExchange) {
        return BindingBuilder.bind(loginOtpQueue).to(mailServiceExchange).with(Constants.LOGIN_OTP_ROUTING_KEY);
    }

    @Bean
    public Binding bindingForgotPasswordQueue(Queue forgotPasswordOtpQueue, TopicExchange mailServiceExchange) {
        return BindingBuilder.bind(forgotPasswordOtpQueue).to(mailServiceExchange).with(Constants.FORGOT_PASSWORD_OTP_ROUTING_KEY);
    }

}