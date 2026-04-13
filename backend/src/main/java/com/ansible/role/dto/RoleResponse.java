package com.ansible.role.dto;

import com.ansible.role.entity.Role;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RoleResponse {

  private final Long id;
  private final Long projectId;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public RoleResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("projectId") Long projectId,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public RoleResponse(Role role) {
    this.id = role.getId();
    this.projectId = role.getProjectId();
    this.name = role.getName();
    this.description = role.getDescription();
    this.createdBy = role.getCreatedBy();
    this.createdAt = role.getCreatedAt();
  }
}
