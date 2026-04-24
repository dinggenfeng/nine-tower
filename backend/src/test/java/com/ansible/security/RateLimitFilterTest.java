package com.ansible.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;
  @Mock private Environment environment;

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter(environment);
  }

  @Test
  void doFilter_nonAuthEndpoint_passesThrough() throws Exception {
    when(environment.matchesProfiles("test")).thenReturn(false);
    when(request.getRequestURI()).thenReturn("/api/projects");
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_loginEndpoint_underLimit_passesThrough() throws Exception {
    when(environment.matchesProfiles("test")).thenReturn(false);
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }
    verify(filterChain, times(10)).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  void doFilter_loginEndpoint_overLimit_returns429() throws Exception {
    when(environment.matchesProfiles("test")).thenReturn(false);
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }
    filter.doFilterInternal(request, response, filterChain);
    verify(response).setStatus(429);
  }
}
