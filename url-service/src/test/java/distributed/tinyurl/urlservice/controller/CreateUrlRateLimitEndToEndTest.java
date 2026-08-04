package distributed.tinyurl.urlservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import distributed.tinyurl.urlservice.TestcontainersConfiguration;
import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.rate-limits.create-url.limit=2",
        "app.rate-limits.create-url.window=1m"
})
class CreateUrlRateLimitEndToEndTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String RATE_LIMIT_KEY = "rate-limit:create-url:" + CLIENT_IP;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(RATE_LIMIT_KEY);
    }

    @Test
    void createShortUrlReturns429WhenClientExceedsLimit() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://www.anthropic.com", null);
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/urls")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr(CLIENT_IP);
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr(CLIENT_IP);
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr(CLIENT_IP);
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }
}
