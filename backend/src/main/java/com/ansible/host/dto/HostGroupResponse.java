package com.ansible.host.dto;

import com.ansible.host.entity.HostGroup;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HostGroupResponse {

  private final Long id;
  private final Long projectId;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public HostGroupResponse(
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

  public HostGroupResponse(HostGroup hostGroup) {
    this.id = hostGroup.getId();
    this.projectId = hostGroup.getProjectId();
    this.name = hostGroup.getName();
    this.description = hostGroup.getDescription();
    this.createdBy = hostGroup.getCreatedBy();
    this.createdAt = hostGroup.getCreatedAt();
  }
}
