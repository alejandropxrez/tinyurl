package distributed.tinyurl.analyticsservice.events;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static distributed.tinyurl.analyticsservice.observability.MetricName.CLICK_EVENTS_CONSUMED;
import static distributed.tinyurl.analyticsservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.analyticsservice.observability.MetricTagValue.SUCCESS;

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
        meterRegistry.counter(CLICK_EVENTS_CONSUMED.key(), OUTCOME.key(), SUCCESS.key()).increment();
    }
}
