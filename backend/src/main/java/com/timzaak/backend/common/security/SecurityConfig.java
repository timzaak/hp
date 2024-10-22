package com.timzaak.backend.common.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig  {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, StringRedisTemplate template) throws Exception {
        var repo = new RedisSecurityContextRepository(template);
        return http
                .csrf((s) -> s.disable())
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext((context) -> context.securityContextRepository(repo))
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .build();

    }
}
