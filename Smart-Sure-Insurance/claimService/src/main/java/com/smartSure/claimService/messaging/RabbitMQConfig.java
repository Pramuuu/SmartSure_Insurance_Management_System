package com.smartSure.claimService.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE             = "smartsure.exchange";
    public static final String CLAIM_DECISION_KEY   = "claim.decision";
    public static final String CLAIM_DECISION_QUEUE = "claim.decision.queue";

    // DLQ constants
    public static final String DLQ_EXCHANGE     = "smartsure.dlx";
    public static final String DLQ_QUEUE        = "smartsure.dlq";
    public static final String DLQ_ROUTING_KEY  = "dlq.claim.decision";

    // ── Main Exchange ─────────────────────────────────────────
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // ── Dead Letter Exchange ──────────────────────────────────
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    // ── Dead Letter Queue ─────────────────────────────────────
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }

    // ── Claim Decision Queue (with DLQ) ───────────────────────
    @Bean
    public Queue claimDecisionQueue() {
        return QueueBuilder.durable(CLAIM_DECISION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding claimDecisionBinding() {
        return BindingBuilder
                .bind(claimDecisionQueue())
                .to(exchange())
                .with(CLAIM_DECISION_KEY);
    }

    // ── Message Converter ─────────────────────────────────────
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ── Rabbit Template ───────────────────────────────────────
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}