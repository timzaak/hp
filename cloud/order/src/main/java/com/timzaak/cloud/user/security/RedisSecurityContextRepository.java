package com.timzaak.cloud.user.security;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;


    public class RedisSecurityContextRepository implements SecurityContextRepository {
    private static final Logger logger = LoggerFactory.getLogger(RedisSecurityContextRepository.class);
    private final StringRedisTemplate operations;

    public RedisSecurityContextRepository(StringRedisTemplate operations) {
        this.operations = operations;
    }

    private @Nullable String readSecurityContextId(HttpServletRequest request) {
        return request.getHeader("Authorization");

    }

    private @Nullable CurrentUser readSecurityContextFromRedis(String text) {
        if(!StringUtils.hasText(text)) {
            return null;
        }
        var id = operations.opsForValue().get("U_" + text);
        if(!StringUtils.hasText(id)) {
            return null;
        }
        return new CurrentUser(Integer.valueOf(id));
    }


    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {

        HttpServletRequest request = requestResponseHolder.getRequest();

        String securityContextId = readSecurityContextId(request);
        var user = readSecurityContextFromRedis(securityContextId);
        if(user == null) {
            return null;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(user);
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        logger.warn("save security context should not be called");
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return readSecurityContextFromRedis(readSecurityContextId(request)) != null;
    }
}
