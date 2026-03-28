package com.smartSure.adminService.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE              = "smartsure.exchange";
    public static final String CLAIM_DECISION_KEY    = "claim.decision";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";

    // Admin listens to claim decisions for audit logging
    public static final String ADMIN_CLAIM_AUDIT_QUEUE   = "admin.claim.audit.queue";
    // Admin listens to payment completions for audit logging
    public static final String ADMIN_PAYMENT_AUDIT_QUEUE = "admin.payment.audit.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue adminClaimAuditQueue() {
        return QueueBuilder.durable(ADMIN_CLAIM_AUDIT_QUEUE).build();
    }

    @Bean
    public Queue adminPaymentAuditQueue() {
        return QueueBuilder.durable(ADMIN_PAYMENT_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding adminClaimAuditBinding() {
        return BindingBuilder.bind(adminClaimAuditQueue()).to(exchange()).with(CLAIM_DECISION_KEY);
    }

    @Bean
    public Binding adminPaymentAuditBinding() {
        return BindingBuilder.bind(adminPaymentAuditQueue()).to(exchange()).with(PAYMENT_COMPLETED_KEY);
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
