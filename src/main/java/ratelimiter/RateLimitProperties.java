package ratelimiter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings read from application.properties under the "ratelimit" prefix. */
@ConfigurationProperties(prefix = "ratelimit")
public record RateLimitProperties(Long capacity, Double refillPerSecond) {

    public RateLimitProperties {
        if (capacity == null) capacity = 10L;
        if (refillPerSecond == null) refillPerSecond = 1.0;
    }
}
