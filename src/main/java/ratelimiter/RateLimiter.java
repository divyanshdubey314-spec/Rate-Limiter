package ratelimiter;

/** Decides whether a request from a given key (user, API key, IP...) is allowed. */
public interface RateLimiter {

    RateLimitResult tryAcquire(String key);

    /** Maximum burst size, handy for response headers. */
    long capacity();
}
