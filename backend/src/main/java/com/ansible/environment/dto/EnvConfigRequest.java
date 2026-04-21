package com.ansible.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvConfigRequest {

  @NotBlank(message = "Config key is required")
  @Size(max = 100, message = "Config key must not exceed 100 characters")
  private String configKey;

  private String configValue;
}
