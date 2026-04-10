package com.ansible.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  private String password;

  @Email(message = "Email format is invalid")
  @Size(max = 100)
  private String email;
}
