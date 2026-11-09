package ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * The "bouncer": runs before every /api/** request.
 * Allowed -> request continues. Blocked -> HTTP 429 and the controller never runs.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    static final String API_KEY_HEADER = "X-API-Key";

    private final RateLimiter limiter;

    public RateLimitInterceptor(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String key = resolveKey(request);
        RateLimitResult result = limiter.tryAcquire(key);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limiter.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.allowed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, (result.retryAfterMillis() + 999) / 1000);
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
        return false;
    }

    /** Identify the caller by API key if given, otherwise by IP address. */
    private String resolveKey(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return "key:" + apiKey;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
