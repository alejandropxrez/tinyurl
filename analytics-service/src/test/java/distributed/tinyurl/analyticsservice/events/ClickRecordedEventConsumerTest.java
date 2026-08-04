package distributed.tinyurl.analyticsservice.events;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClickRecordedEventConsumerTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @Test
    void handleSavesClickEvent() {
        ClickRecordedEventConsumer consumer = new ClickRecordedEventConsumer(clickEventRepository);
        Instant clickedAt = Instant.parse("2026-08-04T12:00:00Z");

        consumer.handle(new ClickRecordedEvent("abc123X", clickedAt));

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());

        ClickEvent saved = captor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("abc123X");
        assertThat(saved.getClickedAt()).isEqualTo(clickedAt);
    }
}
