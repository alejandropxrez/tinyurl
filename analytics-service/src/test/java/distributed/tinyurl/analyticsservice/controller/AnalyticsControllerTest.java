package distributed.tinyurl.analyticsservice.controller;

import distributed.tinyurl.analyticsservice.TestcontainersConfiguration;
import distributed.tinyurl.analyticsservice.model.ClickEvent;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @BeforeEach
    void cleanDatabase() {
        clickEventRepository.deleteAll();
    }

    @Test
    void countClicksReturnsNumberOfEventsForShortCode() throws Exception {
        clickEventRepository.save(ClickEvent.builder()
                .shortCode("abc123X")
                .clickedAt(Instant.parse("2026-08-04T12:00:00Z"))
                .build());
        clickEventRepository.save(ClickEvent.builder()
                .shortCode("abc123X")
                .clickedAt(Instant.parse("2026-08-04T12:01:00Z"))
                .build());
        clickEventRepository.save(ClickEvent.builder()
                .shortCode("other99")
                .clickedAt(Instant.parse("2026-08-04T12:02:00Z"))
                .build());

        mockMvc.perform(get("/api/v1/analytics/urls/{shortCode}/clicks", "abc123X"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc123X"))
                .andExpect(jsonPath("$.clicks").value(2));
    }

    @Test
    void countClicksReturnsZeroWhenShortCodeHasNoEvents() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/urls/{shortCode}/clicks", "missing1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("missing1"))
                .andExpect(jsonPath("$.clicks").value(0));
    }
}
