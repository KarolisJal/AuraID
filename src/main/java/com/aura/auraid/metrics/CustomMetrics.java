package com.aura.auraid.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CustomMetrics {

    private final Counter loginAttempts;
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter registrationAttempts;
    private final Counter registrationSuccess;
    private final Counter registrationFailure;

    public CustomMetrics(MeterRegistry registry) {
        this.loginAttempts = Counter.builder("auth.login.attempts")
                .description("Number of login attempts")
                .register(registry);

        this.loginSuccess = Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .register(registry);

        this.loginFailure = Counter.builder("auth.login.failure")
                .description("Number of failed logins")
                .register(registry);

        this.registrationAttempts = Counter.builder("auth.registration.attempts")
                .description("Number of registration attempts")
                .register(registry);

        this.registrationSuccess = Counter.builder("auth.registration.success")
                .description("Number of successful registrations")
                .register(registry);

        this.registrationFailure = Counter.builder("auth.registration.failure")
                .description("Number of failed registrations")
                .register(registry);
    }
} 