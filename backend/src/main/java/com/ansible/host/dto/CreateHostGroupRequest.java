package com.ansible.host.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHostGroupRequest {

  @NotBlank(message = "Host group name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
