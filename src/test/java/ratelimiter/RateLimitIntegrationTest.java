package ratelimiter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"ratelimit.capacity=3", "ratelimit.refill-per-second=0.001"})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void blocksWithHttp429AfterBurstIsUsed() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/hello").header("X-API-Key", "user-a"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "3"));
        }
        mvc.perform(get("/api/hello").header("X-API-Key", "user-a"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void differentApiKeysAreLimitedSeparately() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(get("/api/hello").header("X-API-Key", "heavy-user"));
        }
        mvc.perform(get("/api/hello").header("X-API-Key", "light-user"))
                .andExpect(status().isOk());
    }
}
