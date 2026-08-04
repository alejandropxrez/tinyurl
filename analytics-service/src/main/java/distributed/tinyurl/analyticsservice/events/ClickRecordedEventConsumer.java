package distributed.tinyurl.analyticsservice.events;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ClickRecordedEventConsumer {

    private final ClickEventRepository clickEventRepository;

    public ClickRecordedEventConsumer(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    @RabbitListener(queues = "${app.events.clicks.queue}")
    public void handle(ClickRecordedEvent event) {
        clickEventRepository.save(ClickEvent.builder()
                .shortCode(event.shortCode())
                .clickedAt(event.clickedAt())
                .build());
    }
}
