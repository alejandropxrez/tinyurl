package distributed.tinyurl.urlservice.events;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {
        RabbitClickEventPublisher.class,
        RabbitClickEventPublisherTest.Config.class
})
@TestPropertySource(properties = {
        "app.events.clicks.exchange=tinyurl.clicks",
        "app.events.clicks.routing-key=click.recorded"
})
class RabbitClickEventPublisherTest {

    private static final String EXCHANGE = "tinyurl.clicks";
    private static final String ROUTING_KEY = "click.recorded";

    @Autowired
    private RabbitClickEventPublisher publisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void resetMocks() {
        reset(rabbitTemplate);
    }

    @Test
    void publishSendsEventToConfiguredExchangeAndRoutingKey() {
        ClickRecordedEvent event = new ClickRecordedEvent("abc123X", Instant.parse("2026-08-04T12:00:00Z"));

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    @Test
    void publishDoesNotThrowWhenRabbitMqFails() {
        ClickRecordedEvent event = new ClickRecordedEvent("abc123X", Instant.parse("2026-08-04T12:00:00Z"));

        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(EXCHANGE, ROUTING_KEY, event);

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
        verify(rabbitTemplate, times(3)).convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    @TestConfiguration
    static class Config {

        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        CircuitBreakerRegistry circuitBreakerRegistry() {
            return CircuitBreakerRegistry.ofDefaults();
        }

        @Bean
        RetryRegistry retryRegistry() {
            return RetryRegistry.ofDefaults();
        }

    }
}
