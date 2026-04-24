package com.ansible.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
  private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);

  public void logLoginFailure(String username, String reason) {
    LOG.warn("Login failed for user '{}': {}", username, reason);
  }

  public void logTokenValidationFailure(String reason) {
    LOG.warn("Token validation failed: {}", reason);
  }

  public void logAccessDenied(Long userId, String resource, Long resourceId) {
    LOG.warn(
        "Access denied: userId={}, resource={}, resourceId={}", userId, resource, resourceId);
  }
}
