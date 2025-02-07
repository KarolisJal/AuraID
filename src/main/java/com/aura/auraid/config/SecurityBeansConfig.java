package com.aura.auraid.config;

import com.aura.auraid.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }
} 