package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleDefaultVariableRequest {

  @NotBlank(message = "Variable key is required")
  @Size(max = 200, message = "Key must not exceed 200 characters")
  private String key;

  private String value;
}
