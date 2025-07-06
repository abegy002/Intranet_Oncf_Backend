package com.example.intranet_back_stage.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class StaticResourceSecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // completely skip Spring Security filters for anything under /uploads/**
        return (web) -> web.ignoring().requestMatchers("/uploads/news-images/**", "/error");
    }
}

