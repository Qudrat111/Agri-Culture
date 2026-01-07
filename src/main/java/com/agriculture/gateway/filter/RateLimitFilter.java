package com.agriculture.gateway.filter;

import com.agriculture.gateway.ratelimit.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter for rate limiting API requests.
 * Returns 429 Too Many Requests when limit exceeded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements Filter {
    
    private final RateLimitService rateLimitService;
    
    @Value("${agriculture.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Use IP address as rate limit key (in production, use user ID if authenticated)
        String key = getClientKey(httpRequest);
        
        if (rateLimitService.isAllowed(key)) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            log.warn("Rate limit exceeded for client: {}", key);
        }
    }
    
    /**
     * Get client identifier for rate limiting.
     */
    private String getClientKey(HttpServletRequest request) {
        // In production, consider using authenticated user ID
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
