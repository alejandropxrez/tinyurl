package distributed.tinyurl.analyticsservice.controller;

import distributed.tinyurl.analyticsservice.TestcontainersConfiguration;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AnalyticsControllerErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClickEventRepository clickEventRepository;

    @Test
    void countClicksReturnsServiceUnavailableWhenDatabaseFails() throws Exception {
        when(clickEventRepository.countByShortCode("abc123X"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        mockMvc.perform(get("/api/v1/analytics/urls/{shortCode}/clicks", "abc123X"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Analytics data is temporarily unavailable"));
    }
}
