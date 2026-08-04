package distributed.tinyurl.analyticsservice.events;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {
        ClickRecordedEventConsumer.class,
        ClickRecordedEventConsumerTest.Config.class
})
class ClickRecordedEventConsumerTest {

    @Autowired
    private ClickRecordedEventConsumer consumer;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(clickEventRepository);
    }

    @Test
    void handleSavesClickEvent() {
        Instant clickedAt = Instant.parse("2026-08-04T12:00:00Z");

        consumer.handle(new ClickRecordedEvent("abc123X", clickedAt));

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());

        ClickEvent saved = captor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("abc123X");
        assertThat(saved.getClickedAt()).isEqualTo(clickedAt);
    }

    @TestConfiguration
    static class Config {

        @Bean
        ClickEventRepository clickEventRepository() {
            return mock(ClickEventRepository.class);
        }
    }
}
