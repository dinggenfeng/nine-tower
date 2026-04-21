package com.ansible.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEnvironmentRequest {

  @NotBlank(message = "Environment name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
