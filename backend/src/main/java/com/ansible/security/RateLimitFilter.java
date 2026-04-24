package com.ansible.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 10;
  private static final String LOGIN_PATH = "/api/auth/login";
  private static final String REGISTER_PATH = "/api/auth/register";

  private final ConcurrentMap<String, RequestCounter> counters = new ConcurrentHashMap<>();
  private final Environment environment;

  public RateLimitFilter(Environment environment) {
    this.environment = environment;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (environment.matchesProfiles("test")) {
      filterChain.doFilter(request, response);
      return;
    }

    String uri = request.getRequestURI();
    if (!LOGIN_PATH.equals(uri) && !REGISTER_PATH.equals(uri)) {
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

  @SuppressWarnings("PMD.AvoidUsingVolatile")
  private static final class RequestCounter {
    private static final long WINDOW_MS = 60_000L;
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    boolean isOverLimit() {
      long now = System.currentTimeMillis();
      if (now - windowStart > WINDOW_MS) {
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
