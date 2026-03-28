package com.smartSure.claimService.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE           = "smartsure.exchange";
    public static final String CLAIM_DECISION_KEY = "claim.decision";
    public static final String CLAIM_DECISION_QUEUE = "claim.decision.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue claimDecisionQueue() {
        return QueueBuilder.durable(CLAIM_DECISION_QUEUE).build();
    }

    @Bean
    public Binding claimDecisionBinding() {
        return BindingBuilder
                .bind(claimDecisionQueue())
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
