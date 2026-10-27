package com.taskmanagement.secure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final int requestsPerMinute;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(@Value("${app.rate-limit.requests-per-minute:20}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean tryConsume(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(requestsPerMinute));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private int tokens;
        private Instant lastRefill;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            Instant now = Instant.now();
            long secondsPassed = java.time.Duration.between(lastRefill, now).getSeconds();
            if (secondsPassed >= 60) {
                tokens = capacity;
                lastRefill = now;
            } else if (secondsPassed > 0) {
                // Add tokens proportional to elapsed time
                int tokensToAdd = (int) (secondsPassed * capacity / 60);
                if (tokensToAdd > 0) {
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefill = now;
                }
            }
        }
    }
}
