package distributed.tinyurl.urlservice.events;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static distributed.tinyurl.urlservice.observability.MetricName.CLICK_EVENTS_PUBLISHED;
import static distributed.tinyurl.urlservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.ERROR;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.SUCCESS;
import static distributed.tinyurl.urlservice.resilience.ResilienceInstance.RABBIT_CLICK_EVENTS;

@Component
public class RabbitClickEventPublisher implements ClickEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public RabbitClickEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.events.clicks.exchange}") String exchange,
            @Value("${app.events.clicks.routing-key}") String routingKey,
            MeterRegistry meterRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RABBIT_CLICK_EVENTS.key());
        this.retry = retryRegistry.retry(RABBIT_CLICK_EVENTS.key());
    }

    @Override
    public void publish(ClickRecordedEvent event) {
        Runnable publish = () -> {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            meterRegistry.counter(CLICK_EVENTS_PUBLISHED.key(), OUTCOME.key(), SUCCESS.key()).increment();
        };

        try {
            CircuitBreaker.decorateRunnable(circuitBreaker, Retry.decorateRunnable(retry, publish)).run();
        } catch (RuntimeException ignored) {
            publishFallback();
        }
    }

    private void publishFallback() {
        meterRegistry.counter(CLICK_EVENTS_PUBLISHED.key(), OUTCOME.key(), ERROR.key()).increment();
        // Analytics must never block the redirect path.
    }
}
