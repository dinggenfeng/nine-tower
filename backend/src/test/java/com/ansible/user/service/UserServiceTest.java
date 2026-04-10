package com.ansible.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.user.dto.UpdateUserRequest;
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
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("encoded");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void getUser_success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    UserResponse response = userService.getUser(1L);

    assertThat(response.getUsername()).isEqualTo("testuser");
    assertThat(response.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void getUser_notFound_throws() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void updateUser_updates_email() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.updateUser(1L, request, 1L);

    verify(userRepository).save(testUser);
    assertThat(testUser.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  void updateUser_forbidden_when_not_owner() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    assertThatThrownBy(() -> userService.updateUser(1L, request, 2L))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("You can only modify your own account");
  }

  @Test
  void updateUser_fails_when_email_taken() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("taken@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.updateUser(1L, request, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email already registered");
  }

  @Test
  void deleteUser_success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    userService.deleteUser(1L, 1L);

    verify(userRepository).delete(testUser);
  }

  @Test
  void deleteUser_forbidden_when_not_owner() {
    assertThatThrownBy(() -> userService.deleteUser(1L, 2L))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("You can only delete your own account");
  }
}
