package com.ansible.role.dto;

import com.ansible.role.entity.Task;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class TaskResponse {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Long id;
  private final Long roleId;
  private final String name;
  private final String module;
  private final String args;
  private final String whenCondition;
  private final String loop;
  private final String until;
  private final String register;
  private final List<String> notify;
  private final Integer taskOrder;
  private final Boolean become;
  private final String becomeUser;
  private final Boolean ignoreErrors;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  @SuppressWarnings("PMD.ExcessiveParameterList")
  public TaskResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("name") String name,
      @JsonProperty("module") String module,
      @JsonProperty("args") String args,
      @JsonProperty("whenCondition") String whenCondition,
      @JsonProperty("loop") String loop,
      @JsonProperty("until") String until,
      @JsonProperty("register") String register,
      @JsonProperty("notify") List<String> notify,
      @JsonProperty("taskOrder") Integer taskOrder,
      @JsonProperty("become") Boolean become,
      @JsonProperty("becomeUser") String becomeUser,
      @JsonProperty("ignoreErrors") Boolean ignoreErrors,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.name = name;
    this.module = module;
    this.args = args;
    this.whenCondition = whenCondition;
    this.loop = loop;
    this.until = until;
    this.register = register;
    this.notify = notify;
    this.taskOrder = taskOrder;
    this.become = become;
    this.becomeUser = becomeUser;
    this.ignoreErrors = ignoreErrors;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public TaskResponse(Task task) {
    this.id = task.getId();
    this.roleId = task.getRoleId();
    this.name = task.getName();
    this.module = task.getModule();
    this.args = task.getArgs();
    this.whenCondition = task.getWhenCondition();
    this.loop = task.getLoop();
    this.until = task.getUntil();
    this.register = task.getRegister();
    this.notify = parseNotify(task.getNotify());
    this.taskOrder = task.getTaskOrder();
    this.become = task.getBecome();
    this.becomeUser = task.getBecomeUser();
    this.ignoreErrors = task.getIgnoreErrors();
    this.createdBy = task.getCreatedBy();
    this.createdAt = task.getCreatedAt();
  }

  private static List<String> parseNotify(String notifyJson) {
    if (notifyJson == null || notifyJson.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(notifyJson, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }
}
