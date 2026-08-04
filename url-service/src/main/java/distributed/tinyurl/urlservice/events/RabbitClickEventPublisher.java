package distributed.tinyurl.urlservice.events;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitClickEventPublisher implements ClickEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitClickEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.events.clicks.exchange}") String exchange,
            @Value("${app.events.clicks.routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(ClickRecordedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (AmqpException ignored) {
            // Analytics must never block the redirect path.
        }
    }
}
