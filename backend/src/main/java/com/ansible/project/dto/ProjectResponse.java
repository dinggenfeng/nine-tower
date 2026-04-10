package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.Project;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProjectResponse {

  private final Long id;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;
  private final ProjectRole myRole;

  @JsonCreator
  public ProjectResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt,
      @JsonProperty("myRole") ProjectRole myRole) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.myRole = myRole;
  }

  public ProjectResponse(Project project, ProjectRole myRole) {
    this.id = project.getId();
    this.name = project.getName();
    this.description = project.getDescription();
    this.createdBy = project.getCreatedBy();
    this.createdAt = project.getCreatedAt();
    this.myRole = myRole;
  }
}
