package com.fasecerta.backend.modules.auth.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int maxAttempts;
    private final long windowSeconds;
    private final Clock clock;
    private final int maxTrackedIps;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private Instant nextCleanup = Instant.MIN;

    @Autowired
    public LoginRateLimiter(@Value("${auth.rate-limit.max-attempts}") int maxAttempts,
                            @Value("${auth.rate-limit.window-seconds}") long windowSeconds) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    public LoginRateLimiter(int maxAttempts, long windowSeconds, Clock clock) {
        this(maxAttempts, windowSeconds, clock, MAX_TRACKED_IPS);
    }

    LoginRateLimiter(int maxAttempts, long windowSeconds, Clock clock, int maxTrackedIps) {
        if (maxAttempts < 1 || windowSeconds < 1 || maxTrackedIps < 1) {
            throw new IllegalArgumentException("A configuração do rate limit deve ser positiva");
        }
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
        this.maxTrackedIps = maxTrackedIps;
    }

    /** Returns an empty value when allowed, or the Retry-After delay when limited. */
    public synchronized OptionalLong retryAfterSeconds(String remoteAddress) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            removeExpired(now);
            nextCleanup = now.plusSeconds(windowSeconds);
        }

        String ip = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
        Bucket bucket = buckets.get(ip);
        if (bucket != null && !now.isBefore(bucket.expiresAt())) {
            buckets.remove(ip);
            bucket = null;
        }

        if (bucket == null) {
            if (buckets.size() >= maxTrackedIps) {
                removeExpired(now);
                if (buckets.size() >= maxTrackedIps) {
                    Instant earliestExpiry = buckets.values().stream()
                            .map(Bucket::expiresAt)
                            .min(Comparator.naturalOrder())
                            .orElseThrow();
                    return OptionalLong.of(retryAfterSeconds(now, earliestExpiry));
                }
            }
            buckets.put(ip, new Bucket(1, now.plusSeconds(windowSeconds)));
            return OptionalLong.empty();
        }

        if (bucket.attempts() >= maxAttempts) {
            return OptionalLong.of(retryAfterSeconds(now, bucket.expiresAt()));
        }
        buckets.put(ip, new Bucket(bucket.attempts() + 1, bucket.expiresAt()));
        return OptionalLong.empty();
    }

    private void removeExpired(Instant now) {
        buckets.values().removeIf(bucket -> !now.isBefore(bucket.expiresAt()));
    }

    private long retryAfterSeconds(Instant now, Instant expiresAt) {
        Duration remaining = Duration.between(now, expiresAt);
        long seconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
        return Math.max(1, seconds);
    }

    private record Bucket(int attempts, Instant expiresAt) {
    }
}
