package distributed.tinyurl.analyticsservice.events;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ClickRecordedEventConsumer {

    private final ClickEventRepository clickEventRepository;
    private final MeterRegistry meterRegistry;

    public ClickRecordedEventConsumer(ClickEventRepository clickEventRepository, MeterRegistry meterRegistry) {
        this.clickEventRepository = clickEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = "${app.events.clicks.queue}")
    public void handle(ClickRecordedEvent event) {
        clickEventRepository.save(ClickEvent.builder()
                .shortCode(event.shortCode())
                .clickedAt(event.clickedAt())
                .build());
        meterRegistry.counter("tinyurl_click_events_consumed_total", "outcome", "success").increment();
    }
}
