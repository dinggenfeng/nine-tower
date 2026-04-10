package com.ansible.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.security.JwtTokenProvider;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private AuthService authService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("encoded_password");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void register_success() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("new@example.com");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtTokenProvider.generateToken(1L)).thenReturn("jwt_token");

    TokenResponse response = authService.register(request);

    assertThat(response.getToken()).isEqualTo("jwt_token");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getUser().getUsername()).isEqualTo("testuser");
  }

  @Test
  void register_fails_when_username_taken() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("testuser");
    request.setPassword("password123");
    request.setEmail("other@example.com");

    when(userRepository.existsByUsername("testuser")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username already taken");

    verify(userRepository, never()).save(any());
  }

  @Test
  void register_fails_when_email_taken() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("test@example.com");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email already registered");

    verify(userRepository, never()).save(any());
  }

  @Test
  void login_success() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("password123");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
    when(jwtTokenProvider.generateToken(1L)).thenReturn("jwt_token");

    TokenResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("jwt_token");
    assertThat(response.getUser().getUsername()).isEqualTo("testuser");
  }

  @Test
  void login_fails_when_user_not_found() {
    LoginRequest request = new LoginRequest();
    request.setUsername("unknown");
    request.setPassword("password123");

    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid username or password");
  }

  @Test
  void login_fails_when_password_wrong() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("wrongpassword");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid username or password");
  }
}