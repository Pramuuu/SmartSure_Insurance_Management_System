package com.smartSure.PolicyService.service;

import com.smartSure.PolicyService.config.RabbitMQConfig;
import com.smartSure.PolicyService.dto.event.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPublisher Unit Tests")
class NotificationPublisherTest {

    @Mock  private RabbitTemplate      rabbitTemplate;
    @InjectMocks private NotificationPublisher notificationPublisher;

    @Nested
    @DisplayName("publishPolicyPurchased()")
    class PublishPolicyPurchasedTests {

        @Test
        @DisplayName("should send to correct exchange and routing key")
        void publish_sendsToCorrectExchangeAndKey() {
            PolicyPurchasedEvent event = PolicyPurchasedEvent.builder()
                    .policyId(1L).policyNumber("POL-20250101-ABCDE")
                    .customerId(42L).customerEmail("c@email.com")
                    .policyTypeName("Health Insurance")
                    .coverageAmount(new BigDecimal("500000.00"))
                    .premiumAmount(new BigDecimal("2625.00"))
                    .paymentFrequency("MONTHLY")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(12))
                    .status("ACTIVE")
                    .build();

            notificationPublisher.publishPolicyPurchased(event);

            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.KEY_POLICY_PURCHASED),
                    any(PolicyPurchasedEvent.class));
        }

        @Test
        @DisplayName("should set publishedAt before sending")
        void publish_setsPublishedAt() {
            PolicyPurchasedEvent event = PolicyPurchasedEvent.builder()
                    .policyId(1L).customerId(42L).build();
            assertThat(event.getPublishedAt()).isNull();

            notificationPublisher.publishPolicyPurchased(event);

            assertThat(event.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("should NOT throw when RabbitMQ broker is down")
        void publish_rabbitMQDown_doesNotThrow() {
            doThrow(new RuntimeException("Connection refused"))
                    .when(rabbitTemplate).convertAndSend(any(), any(String.class), any(Object.class));

            PolicyPurchasedEvent event = PolicyPurchasedEvent.builder()
                    .policyId(1L).customerId(42L).build();

            assertThatCode(() -> notificationPublisher.publishPolicyPurchased(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishPolicyCancelled()")
    class PublishPolicyCancelledTests {

        @Test
        @DisplayName("should send to POLICY_CANCELLED routing key")
        void publish_sendsToCorrectKey() {
            PolicyCancelledEvent event = PolicyCancelledEvent.builder()
                    .policyId(1L).customerId(42L)
                    .policyNumber("POL-20250101-ABCDE")
                    .cancellationReason("Test").build();

            notificationPublisher.publishPolicyCancelled(event);

            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.KEY_POLICY_CANCELLED),
                    any(PolicyCancelledEvent.class));
        }

        @Test
        @DisplayName("should NOT throw when RabbitMQ broker is down")
        void publish_rabbitMQDown_doesNotThrow() {
            doThrow(new RuntimeException("Broker down"))
                    .when(rabbitTemplate).convertAndSend(any(), any(String.class), any(Object.class));

            PolicyCancelledEvent event = PolicyCancelledEvent.builder()
                    .policyId(1L).customerId(42L).build();

            assertThatCode(() -> notificationPublisher.publishPolicyCancelled(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishPremiumPaid()")
    class PublishPremiumPaidTests {

        @Test
        @DisplayName("should send to PREMIUM_PAID routing key")
        void publish_sendsToCorrectKey() {
            PremiumPaidEvent event = PremiumPaidEvent.builder()
                    .premiumId(1L).policyId(1L).customerId(42L)
                    .amount(new BigDecimal("2625.00"))
                    .paidDate(LocalDate.now()).build();

            notificationPublisher.publishPremiumPaid(event);

            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.KEY_PREMIUM_PAID),
                    any(PremiumPaidEvent.class));
        }

        @Test
        @DisplayName("should NOT throw when RabbitMQ broker is down")
        void publish_rabbitMQDown_doesNotThrow() {
            doThrow(new RuntimeException("Broker down"))
                    .when(rabbitTemplate).convertAndSend(any(), any(String.class), any(Object.class));

            PremiumPaidEvent event = PremiumPaidEvent.builder()
                    .premiumId(1L).policyId(1L).customerId(42L).build();

            assertThatCode(() -> notificationPublisher.publishPremiumPaid(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishPremiumDueReminder()")
    class PublishPremiumDueReminderTests {

        @Test
        @DisplayName("should send to PREMIUM_DUE_REMINDER routing key")
        void publish_sendsToCorrectKey() {
            PremiumDueReminderEvent event = PremiumDueReminderEvent.builder()
                    .premiumId(1L).policyId(1L).customerId(42L)
                    .amount(new BigDecimal("2625.00"))
                    .dueDate(LocalDate.now().plusDays(7)).build();

            notificationPublisher.publishPremiumDueReminder(event);

            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.KEY_PREMIUM_DUE_REMINDER),
                    any(PremiumDueReminderEvent.class));
        }
    }
}
