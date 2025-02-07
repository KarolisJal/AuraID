package com.aura.auraid.config;

import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    public Bucket createNewBucket() {
        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(20)
                .refillIntervally(20, Duration.ofMinutes(1))
                .initialTokens(20)
            )
            .build();
    }

    public Bucket createAuthBucket() {
        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .initialTokens(5)
            )
            .build();
    }
} 