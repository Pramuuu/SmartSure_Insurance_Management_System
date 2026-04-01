package com.smartSure.paymentService.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String EXCHANGE = "smartsure.exchange";

    // Routing keys
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String CLAIM_DECISION_KEY    = "claim.decision";

    // Queues
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";
    public static final String CLAIM_PAYOUT_QUEUE      = "payment.claim.payout.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build();
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder
                .bind(paymentCompletedQueue())
                .to(exchange())
                .with(PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Queue claimPayoutQueue() {
        return QueueBuilder.durable(CLAIM_PAYOUT_QUEUE).build();
    }

    @Bean
    public Binding claimPayoutBinding() {
        return BindingBuilder
                .bind(claimPayoutQueue())
                .to(exchange())
                .with(CLAIM_DECISION_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
