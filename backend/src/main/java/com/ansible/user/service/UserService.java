package com.ansible.user.service;

import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public UserResponse getUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return new UserResponse(user);
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
    return userRepository.searchUsers(keyword, pageable).map(UserResponse::new);
  }

  @Transactional
  public UserResponse updateUser(Long userId, UpdateUserRequest request, Long currentUserId) {
    if (!userId.equals(currentUserId)) {
      throw new SecurityException("You can only modify your own account");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (StringUtils.hasText(request.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email already registered");
      }
      user.setEmail(request.getEmail());
    }
    if (StringUtils.hasText(request.getPassword())) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    return new UserResponse(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long userId, Long currentUserId) {
    if (!userId.equals(currentUserId)) {
      throw new SecurityException("You can only delete your own account");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    userRepository.delete(user);
  }
}
