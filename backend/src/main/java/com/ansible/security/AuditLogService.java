package com.ansible.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

  public void logLoginFailure(String username, String reason) {
    log.warn("Login failed for user '{}': {}", username, reason);
  }

  public void logTokenValidationFailure(String reason) {
    log.warn("Token validation failed: {}", reason);
  }

  public void logAccessDenied(Long userId, String resource, Long resourceId) {
    log.warn(
        "Access denied: userId={}, resource={}, resourceId={}", userId, resource, resourceId);
  }
}
