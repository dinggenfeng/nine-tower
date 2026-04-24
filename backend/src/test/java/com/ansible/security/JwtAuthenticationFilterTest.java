package com.ansible.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private UserDetailsServiceImpl userDetailsService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter filter;

  @BeforeEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_noAuthorizationHeader_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
  }

  @Test
  void doFilterInternal_invalidBearerToken_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
    when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
  }

  @Test
  void doFilterInternal_validBearerToken_setsAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
    when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(42L);
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetailsService.loadUserById(42L)).thenReturn(userDetails);

    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isEqualTo(userDetails);
  }

  @Test
  void doFilterInternal_nonBearerHeader_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic abc123");
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
