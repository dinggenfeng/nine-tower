package com.ansible.environment.dto;

import com.ansible.environment.entity.EnvConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class EnvConfigResponse {

  private final Long id;
  private final Long environmentId;
  private final String configKey;
  private final String configValue;

  @JsonCreator
  public EnvConfigResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("environmentId") Long environmentId,
      @JsonProperty("configKey") String configKey,
      @JsonProperty("configValue") String configValue) {
    this.id = id;
    this.environmentId = environmentId;
    this.configKey = configKey;
    this.configValue = configValue;
  }

  public EnvConfigResponse(EnvConfig config) {
    this.id = config.getId();
    this.environmentId = config.getEnvironmentId();
    this.configKey = config.getConfigKey();
    this.configValue = config.getConfigValue();
  }
}
