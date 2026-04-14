package com.ansible.role.dto;

import com.ansible.role.entity.Handler;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HandlerResponse {

  private final Long id;
  private final Long roleId;
  private final String name;
  private final String module;
  private final String args;
  private final String whenCondition;
  private final String register;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public HandlerResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("name") String name,
      @JsonProperty("module") String module,
      @JsonProperty("args") String args,
      @JsonProperty("whenCondition") String whenCondition,
      @JsonProperty("register") String register,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.name = name;
    this.module = module;
    this.args = args;
    this.whenCondition = whenCondition;
    this.register = register;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public HandlerResponse(Handler handler) {
    this.id = handler.getId();
    this.roleId = handler.getRoleId();
    this.name = handler.getName();
    this.module = handler.getModule();
    this.args = handler.getArgs();
    this.whenCondition = handler.getWhenCondition();
    this.register = handler.getRegister();
    this.createdBy = handler.getCreatedBy();
    this.createdAt = handler.getCreatedAt();
  }
}
