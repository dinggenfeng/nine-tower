package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {

  @NotBlank(message = "Task name is required")
  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @NotBlank(message = "Module is required")
  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 500, message = "Loop must not exceed 500 characters")
  private String loop;

  @Size(max = 500, message = "Until must not exceed 500 characters")
  private String until;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;

  private List<String> notify;

  @NotNull(message = "Task order is required")
  private Integer taskOrder;

  private Boolean become;

  @Size(max = 100, message = "Become user must not exceed 100 characters")
  private String becomeUser;
}
