package com.ansible.role.dto;

import com.ansible.role.entity.RoleVariable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RoleVariableResponse {

  private final Long id;
  private final Long roleId;
  private final String key;
  private final String value;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public RoleVariableResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("key") String key,
      @JsonProperty("value") String value,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.key = key;
    this.value = value;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public RoleVariableResponse(RoleVariable variable) {
    this.id = variable.getId();
    this.roleId = variable.getRoleId();
    this.key = variable.getKey();
    this.value = variable.getValue();
    this.createdBy = variable.getCreatedBy();
    this.createdAt = variable.getCreatedAt();
  }
}
