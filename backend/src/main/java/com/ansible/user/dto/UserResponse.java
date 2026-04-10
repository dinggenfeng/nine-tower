package com.ansible.user.dto;

import com.ansible.user.entity.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserResponse {

  private final Long id;
  private final String username;
  private final String email;
  private final LocalDateTime createdAt;

  @JsonCreator
  public UserResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("username") String username,
      @JsonProperty("email") String email,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.createdAt = createdAt;
  }

  public UserResponse(User user) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.createdAt = user.getCreatedAt();
  }
}
