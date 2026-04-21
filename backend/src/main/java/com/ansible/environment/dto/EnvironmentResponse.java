package com.ansible.environment.dto;

import com.ansible.environment.entity.Environment;
import com.ansible.environment.entity.EnvConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class EnvironmentResponse {

  private final Long id;
  private final Long projectId;
  private final String name;
  private final String description;
  private final List<EnvConfigResponse> configs;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  @JsonCreator
  public EnvironmentResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("projectId") Long projectId,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("configs") List<EnvConfigResponse> configs,
      @JsonProperty("createdAt") LocalDateTime createdAt,
      @JsonProperty("updatedAt") LocalDateTime updatedAt) {
    this.id = id;
    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.configs = configs;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public EnvironmentResponse(Environment env, List<EnvConfig> configs) {
    this.id = env.getId();
    this.projectId = env.getProjectId();
    this.name = env.getName();
    this.description = env.getDescription();
    this.configs =
        configs.stream().map(EnvConfigResponse::new).toList();
    this.createdAt = env.getCreatedAt();
    this.updatedAt = env.getUpdatedAt();
  }
}
