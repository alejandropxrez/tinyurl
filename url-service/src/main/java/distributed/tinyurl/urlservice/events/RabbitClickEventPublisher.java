package distributed.tinyurl.urlservice.events;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static distributed.tinyurl.urlservice.observability.MetricName.CLICK_EVENTS_PUBLISHED;
import static distributed.tinyurl.urlservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.ERROR;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.SUCCESS;

@Component
public class RabbitClickEventPublisher implements ClickEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final MeterRegistry meterRegistry;

    public RabbitClickEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.events.clicks.exchange}") String exchange,
            @Value("${app.events.clicks.routing-key}") String routingKey,
            MeterRegistry meterRegistry
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(ClickRecordedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            meterRegistry.counter(CLICK_EVENTS_PUBLISHED.key(), OUTCOME.key(), SUCCESS.key()).increment();
        } catch (AmqpException ignored) {
            meterRegistry.counter(CLICK_EVENTS_PUBLISHED.key(), OUTCOME.key(), ERROR.key()).increment();
            // Analytics must never block the redirect path.
        }
    }
}
