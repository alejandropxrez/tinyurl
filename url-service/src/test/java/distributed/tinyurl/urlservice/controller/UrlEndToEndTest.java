package distributed.tinyurl.urlservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import distributed.tinyurl.urlservice.TestcontainersConfiguration;
import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UrlEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void createResolveAndFetchStatsFullFlow() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://www.anthropic.com", null);

        String responseJson = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andExpect(jsonPath("$.originalUrl").value("https://www.anthropic.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shortCode = jsonMapper.readTree(responseJson).get("shortCode").asText();

        mockMvc.perform(get("/{code}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.anthropic.com"));

        mockMvc.perform(get("/api/v1/urls/{code}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    void redirectReturns404WhenShortCodeDoesNotExist() throws Exception {
        mockMvc.perform(get("/{code}", "noexiste123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void redirectReturns410WhenUrlExpired() throws Exception {
        CreateUrlRequest expiredRequest = new CreateUrlRequest(
                "https://www.anthropic.com",
                Instant.now().minusSeconds(3600) // expired
        );

        String responseJson = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(expiredRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shortCode = jsonMapper.readTree(responseJson).get("shortCode").asText();

        mockMvc.perform(get("/{code}", shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    void createShortUrlReturns400WhenOriginalUrlIsInvalid() throws Exception {
        CreateUrlRequest invalidRequest = new CreateUrlRequest("no-es-una-url", null);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createShortUrlReturns400WhenOriginalUrlIsBlank() throws Exception {
        CreateUrlRequest invalidRequest = new CreateUrlRequest("", null);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatsReturns404WhenShortCodeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/urls/{code}", "noexiste123"))
                .andExpect(status().isNotFound());
    }
}
