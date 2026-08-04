package distributed.tinyurl.analyticsservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange clickEventsExchange(@Value("${app.events.clicks.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    Queue clickEventsQueue(@Value("${app.events.clicks.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    Binding clickEventsBinding(
            Queue clickEventsQueue,
            DirectExchange clickEventsExchange,
            @Value("${app.events.clicks.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(clickEventsQueue).to(clickEventsExchange).with(routingKey);
    }
}
