package com.ansible.user.dto;

import com.ansible.user.entity.User;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserResponse {

  private final Long id;
  private final String username;
  private final String email;
  private final LocalDateTime createdAt;

  public UserResponse(User user) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.createdAt = user.getCreatedAt();
  }
}
