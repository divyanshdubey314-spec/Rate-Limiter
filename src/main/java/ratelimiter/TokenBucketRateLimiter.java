package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Keeps one {@link TokenBucket} per key (for example per API key or IP address).
 *
 * <p>{@code computeIfAbsent} on a ConcurrentHashMap guarantees that two threads
 * asking for the same new key end up sharing a single bucket.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    public TokenBucketRateLimiter(long capacity, double refillPerSecond, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.nanoClock = nanoClock;
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(
                key, k -> new TokenBucket(capacity, refillPerSecond, nanoClock));
        return bucket.tryConsume();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    /** Number of distinct keys currently tracked (useful for tests and metrics). */
    public int trackedKeys() {
        return buckets.size();
    }
}
