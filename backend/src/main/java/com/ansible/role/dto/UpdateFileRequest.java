package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFileRequest {

  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 500, message = "Parent directory must not exceed 500 characters")
  private String parentDir;

  @Size(max = 500, message = "Target path must not exceed 500 characters")
  private String targetPath;

  private String textContent;
}
