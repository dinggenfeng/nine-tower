package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockChildRequest {

  @NotBlank(message = "Section is required")
  @Pattern(
      regexp = "^(BLOCK|RESCUE|ALWAYS)$",
      message = "Section must be one of BLOCK, RESCUE, ALWAYS")
  private String section;

  @NotBlank(message = "Child task name is required")
  @Size(max = 200)
  private String name;

  @NotBlank(message = "Module is required")
  @Size(max = 100)
  private String module;

  private String args;

  @Size(max = 500)
  private String whenCondition;

  @Size(max = 500)
  private String loop;

  @Size(max = 500)
  private String until;

  @Size(max = 100)
  private String register;

  private String notify; // JSON array string

  @NotNull(message = "Task order is required")
  private Integer taskOrder;

  private Boolean become;

  @Size(max = 100)
  private String becomeUser;

  private Boolean ignoreErrors;
}
