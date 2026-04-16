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
    public static final String CLAIM_PAYOUT_QUEUE = "payment.claim.payout.queue";

    // DLQ
    public static final String DLQ_EXCHANGE = "smartsure.dlx";
    public static final String DLQ_QUEUE    = "smartsure.dlq";
    public static final String DLQ_ROUTING_KEY = "dlq.claim.payout";

    // =========================
    // MAIN EXCHANGE
    // =========================
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // =========================
    // DEAD LETTER EXCHANGE
    // =========================
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    // =========================
    // DEAD LETTER QUEUE
    // =========================
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#"); // catch all
    }

    // =========================
    // MAIN QUEUE (WITH DLQ)
    // =========================
    @Bean
    public Queue claimPayoutQueue() {
        return QueueBuilder.durable(CLAIM_PAYOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding claimPayoutBinding() {
        return BindingBuilder
                .bind(claimPayoutQueue())
                .to(exchange())
                .with(CLAIM_DECISION_KEY);
    }

    // =========================
    // MESSAGE CONVERTER
    // =========================
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // =========================
    // RABBIT TEMPLATE
    // =========================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}