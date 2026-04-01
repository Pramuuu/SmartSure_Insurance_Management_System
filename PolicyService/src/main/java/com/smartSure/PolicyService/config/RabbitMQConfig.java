package com.smartSure.PolicyService.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for PolicyService.
 *
 * CRITICAL FIX: PolicyService previously used a separate exchange
 * "smartsure.notifications" while PaymentService publishes to "smartsure.exchange".
 * This caused the PaymentCompletedEvent to NEVER reach PolicyService,
 * meaning premium payments were never marked as PAID.
 *
 * All services now share ONE exchange: "smartsure.exchange" (Topic Exchange).
 *
 * PolicyService publishes:
 *   policy.purchased   → notification.policy.purchased queue
 *   policy.cancelled   → notification.policy.cancelled queue
 *   premium.due.reminder → notification.premium.due.reminder queue
 *   policy.expiry.reminder → notification.policy.expiry.reminder queue
 *
 * PolicyService consumes:
 *   payment.completed  → notification.premium.paid queue (marks premium PAID)
 *
 * Dead Letter Queue:
 *   All queues route failed messages to smartsure.dlx exchange → smartsure.dlq
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange names ──────────────────────────────────────────────────
    // FIXED: unified to same exchange as all other services
    public static final String EXCHANGE     = "smartsure.exchange";
    public static final String DLQ_EXCHANGE = "smartsure.dlx";

    // ── Queue names ─────────────────────────────────────────────────────
    public static final String QUEUE_POLICY_PURCHASED        = "notification.policy.purchased";
    public static final String QUEUE_PREMIUM_PAID            = "notification.premium.paid";
    public static final String QUEUE_POLICY_CANCELLED        = "notification.policy.cancelled";
    public static final String QUEUE_PREMIUM_DUE_REMINDER    = "notification.premium.due.reminder";
    public static final String QUEUE_POLICY_EXPIRY_REMINDER  = "notification.policy.expiry.reminder";
    public static final String DLQ                           = "smartsure.dlq";

    // ── Routing keys ─────────────────────────────────────────────────────
    public static final String KEY_POLICY_PURCHASED       = "policy.purchased";
    // FIXED: PaymentService publishes "payment.completed" to smartsure.exchange
    // Now PolicyService listens on the same exchange with the same routing key
    public static final String KEY_PREMIUM_PAID           = "payment.completed";
    public static final String KEY_POLICY_CANCELLED       = "policy.cancelled";
    public static final String KEY_PREMIUM_DUE_REMINDER   = "premium.due.reminder";
    public static final String KEY_POLICY_EXPIRY_REMINDER = "policy.expiry.reminder";

    // ── Main exchange ────────────────────────────────────────────────────
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // ── Dead Letter Exchange + Queue ─────────────────────────────────────
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }

    // ── Policy Purchased Queue ───────────────────────────────────────────
    @Bean
    public Queue policyPurchasedQueue() {
        return QueueBuilder.durable(QUEUE_POLICY_PURCHASED)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.policy.purchased")
                .build();
    }

    @Bean
    public Binding policyPurchasedBinding() {
        return BindingBuilder.bind(policyPurchasedQueue()).to(exchange()).with(KEY_POLICY_PURCHASED);
    }

    // ── Premium Paid Queue (FIXED: now bound to smartsure.exchange) ──────
    @Bean
    public Queue premiumPaidQueue() {
        return QueueBuilder.durable(QUEUE_PREMIUM_PAID)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.premium.paid")
                .build();
    }

    @Bean
    public Binding premiumPaidBinding() {
        // Consumes payment.completed events from the unified exchange
        return BindingBuilder.bind(premiumPaidQueue()).to(exchange()).with(KEY_PREMIUM_PAID);
    }

    // ── Policy Cancelled Queue ───────────────────────────────────────────
    @Bean
    public Queue policyCancelledQueue() {
        return QueueBuilder.durable(QUEUE_POLICY_CANCELLED)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.policy.cancelled")
                .build();
    }

    @Bean
    public Binding policyCancelledBinding() {
        return BindingBuilder.bind(policyCancelledQueue()).to(exchange()).with(KEY_POLICY_CANCELLED);
    }

    // ── Premium Due Reminder Queue ───────────────────────────────────────
    @Bean
    public Queue premiumDueReminderQueue() {
        return QueueBuilder.durable(QUEUE_PREMIUM_DUE_REMINDER)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.premium.due.reminder")
                .build();
    }

    @Bean
    public Binding premiumDueReminderBinding() {
        return BindingBuilder.bind(premiumDueReminderQueue()).to(exchange()).with(KEY_PREMIUM_DUE_REMINDER);
    }

    // ── Policy Expiry Reminder Queue ─────────────────────────────────────
    @Bean
    public Queue policyExpiryReminderQueue() {
        return QueueBuilder.durable(QUEUE_POLICY_EXPIRY_REMINDER)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.policy.expiry.reminder")
                .build();
    }

    @Bean
    public Binding policyExpiryReminderBinding() {
        return BindingBuilder.bind(policyExpiryReminderQueue()).to(exchange()).with(KEY_POLICY_EXPIRY_REMINDER);
    }

    // ── JSON Message Converter ───────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}