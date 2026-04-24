package com.ansible.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 10;
  private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (!uri.equals("/api/auth/login") && !uri.equals("/api/auth/register")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = request.getRemoteAddr();
    RequestCounter counter = counters.computeIfAbsent(clientIp, k -> new RequestCounter());

    if (counter.isOverLimit()) {
      response.setStatus(429);
      return;
    }

    counter.increment();
    filterChain.doFilter(request, response);
  }

  private static class RequestCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    boolean isOverLimit() {
      long now = System.currentTimeMillis();
      if (now - windowStart > 60000) {
        count.set(0);
        windowStart = now;
      }
      return count.get() >= MAX_REQUESTS_PER_MINUTE;
    }

    void increment() {
      count.incrementAndGet();
    }
  }
}
