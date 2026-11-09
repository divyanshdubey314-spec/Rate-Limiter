package ratelimiter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    @Test
    void keysHaveIndependentBuckets() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1.0, clock::get);

        assertTrue(limiter.tryAcquire("alice").allowed());
        assertTrue(limiter.tryAcquire("alice").allowed());
        assertFalse(limiter.tryAcquire("alice").allowed());   // alice is out

        assertTrue(limiter.tryAcquire("bob").allowed());      // bob is unaffected
        assertEquals(2, limiter.trackedKeys());
    }
}
