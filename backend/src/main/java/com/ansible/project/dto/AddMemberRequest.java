package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

  @NotNull(message = "User ID is required")
  private Long userId;

  @NotNull(message = "Role is required")
  private ProjectRole role;
}
