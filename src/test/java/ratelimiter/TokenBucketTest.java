package ratelimiter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void allowsBurstUpToCapacityThenRejects() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(5, 1.0, clock::get);

        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume().allowed(), "request " + i + " should pass");
        }
        RateLimitResult blocked = bucket.tryConsume();
        assertFalse(blocked.allowed());
        assertEquals(1000, blocked.retryAfterMillis());
    }

    @Test
    void refillsOverTime() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(2, 1.0, clock::get);

        bucket.tryConsume();
        bucket.tryConsume();
        assertFalse(bucket.tryConsume().allowed());

        clock.addAndGet(SECOND);                       // one second passes -> one token
        assertTrue(bucket.tryConsume().allowed());
        assertFalse(bucket.tryConsume().allowed());
    }

    @Test
    void neverRefillsAboveCapacity() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(3, 10.0, clock::get);

        clock.addAndGet(60 * SECOND);                  // a long idle period
        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (bucket.tryConsume().allowed()) allowed++;
        }
        assertEquals(3, allowed);
    }

    @Test
    void reportsRemainingTokens() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(3, 1.0, clock::get);
        assertEquals(2, bucket.tryConsume().remaining());
        assertEquals(1, bucket.tryConsume().remaining());
        assertEquals(0, bucket.tryConsume().remaining());
    }

    @Test
    void rejectsInvalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, 0));
    }

    /** The key concurrency test: 200 threads race for 10 tokens; exactly 10 may win. */
    @Test
    void isThreadSafeUnderContention() throws Exception {
        AtomicLong frozenClock = new AtomicLong(0);    // no refill during the race
        TokenBucket bucket = new TokenBucket(10, 1.0, frozenClock::get);

        int threads = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (bucket.tryConsume().allowed()) {
                    allowed.incrementAndGet();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(10, allowed.get());
    }
}
