package com.ansible.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  @Mock private AuditLogService auditLogService;

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(auditLogService);
    ReflectionTestUtils.setField(
        provider, "jwtSecret", "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
    ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3600000L);
    provider.init();
  }

  @Test
  void generateToken_andGetUserIdFromToken() {
    Long userId = 42L;
    String token = provider.generateToken(userId);
    Long extracted = provider.getUserIdFromToken(token);
    assertThat(extracted).isEqualTo(userId);
  }

  @Test
  void validateToken_validToken_returnsTrue() {
    String token = provider.generateToken(1L);
    assertThat(provider.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_invalidToken_returnsFalse() {
    assertThat(provider.validateToken("invalid.token.here")).isFalse();
  }

  @Test
  void validateToken_expiredToken_returnsFalse() {
    JwtTokenProvider expiredProvider = new JwtTokenProvider(auditLogService);
    ReflectionTestUtils.setField(
        expiredProvider,
        "jwtSecret",
        "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
    ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -1000L);
    expiredProvider.init();
    String token = expiredProvider.generateToken(1L);
    assertThat(provider.validateToken(token)).isFalse();
  }

  @Test
  void validateToken_emptyString_returnsFalse() {
    assertThat(provider.validateToken("")).isFalse();
  }

  @Test
  void validateToken_null_returnsFalse() {
    assertThat(provider.validateToken(null)).isFalse();
  }
}
