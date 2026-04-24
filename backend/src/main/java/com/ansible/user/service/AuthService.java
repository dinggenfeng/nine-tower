package com.ansible.user.service;

import com.ansible.security.AuditLogService;
import com.ansible.security.JwtTokenProvider;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuditLogService auditLogService;

  @Value("${app.jwt.expiration-ms}")
  private long jwtExpirationMs;

  @Transactional
  public TokenResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setEmail(request.getEmail());
    User saved = userRepository.save(user);
    String token = jwtTokenProvider.generateToken(saved.getId());
    return new TokenResponse(token, "Bearer", jwtExpirationMs, new UserResponse(saved));
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(
                () -> {
                  auditLogService.logLoginFailure(request.getUsername(), "user not found");
                  return new IllegalArgumentException("Invalid username or password");
                });
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      auditLogService.logLoginFailure(request.getUsername(), "wrong password");
      throw new IllegalArgumentException("Invalid username or password");
    }
    String token = jwtTokenProvider.generateToken(user.getId());
    return new TokenResponse(token, "Bearer", jwtExpirationMs, new UserResponse(user));
  }
}