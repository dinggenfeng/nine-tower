package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHandlerRequest {

  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;

  private Boolean become;

  @Size(max = 100, message = "Become user must not exceed 100 characters")
  private String becomeUser;
}
