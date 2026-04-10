package com.ansible.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {

  private String token;
  private String tokenType;
  private long expiresIn;
  private UserResponse user;
}
