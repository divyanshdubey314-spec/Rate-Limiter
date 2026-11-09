package ratelimiter;

import java.util.function.LongSupplier;

/**
 * A single token bucket.
 *
 * <p>The bucket holds up to {@code capacity} tokens and refills continuously at
 * {@code refillPerSecond}. Every request takes one token; with no token the
 * request is rejected.
 *
 * <p>Thread safety: all state changes happen inside {@code synchronized} methods,
 * so two threads can never both take the last token. We do not run a background
 * refill thread. Instead, tokens are "topped up" lazily on each call based on the
 * time elapsed since the last refill, which is cheaper and simpler.
 */
public class TokenBucket {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final long capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;

    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    /** The clock is injectable so tests can control time instead of sleeping. */
    public TokenBucket(long capacity, double refillPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillPerSecond <= 0) {
            throw new IllegalArgumentException("refillPerSecond must be positive");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.nanoClock = nanoClock;
        this.tokens = capacity;                 // start full so first users can burst
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    public synchronized RateLimitResult tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return new RateLimitResult(true, (long) tokens, 0);
        }
        double missing = 1.0 - tokens;
        long retryAfterMillis = (long) Math.ceil(missing / refillPerSecond * 1000.0);
        return new RateLimitResult(false, 0, retryAfterMillis);
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos <= 0) {
            return;
        }
        double newTokens = (double) elapsedNanos / NANOS_PER_SECOND * refillPerSecond;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefillNanos = now;
    }

    public long capacity() {
        return capacity;
    }
}
