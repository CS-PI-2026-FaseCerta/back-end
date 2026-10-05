package com.fasecerta.backend.modules.auth.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {
    private final MutableClock clock = new MutableClock();

    @Test
    void allowsConfiguredAttemptsThenRecoversAfterWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 60, clock);

        assertTrue(limiter.retryAfterSeconds("192.0.2.1").isEmpty());
        assertTrue(limiter.retryAfterSeconds("192.0.2.1").isEmpty());
        assertEquals(60, limiter.retryAfterSeconds("192.0.2.1").orElseThrow());

        clock.advanceSeconds(30);
        assertEquals(30, limiter.retryAfterSeconds("192.0.2.1").orElseThrow());

        clock.advanceSeconds(30);
        assertTrue(limiter.retryAfterSeconds("192.0.2.1").isEmpty());
    }

    @Test
    void keepsIpsIndependentAndRemovesExpiredBucketsAtCapacity() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60, clock, 2);

        assertTrue(limiter.retryAfterSeconds("192.0.2.1").isEmpty());
        assertTrue(limiter.retryAfterSeconds("192.0.2.2").isEmpty());
        assertEquals(60, limiter.retryAfterSeconds("192.0.2.1").orElseThrow());
        assertEquals(60, limiter.retryAfterSeconds("192.0.2.3").orElseThrow());

        clock.advanceSeconds(60);
        assertTrue(limiter.retryAfterSeconds("192.0.2.3").isEmpty());
    }

    @Test
    void concurrentRequestsCannotExceedTheLimit() throws Exception {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 60, clock);
        var executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return limiter.retryAfterSeconds("192.0.2.1").isEmpty();
                }));
            }
            start.countDown();
            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(5, accepted);
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        void advanceSeconds(long seconds) {
            now.updateAndGet(instant -> instant.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
