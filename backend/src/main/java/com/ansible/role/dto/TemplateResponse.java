package com.ansible.role.dto;

import com.ansible.role.entity.Template;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class TemplateResponse {

  private final Long id;
  private final Long roleId;
  private final String parentDir;
  private final String name;
  private final String targetPath;
  private final String content;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public TemplateResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("parentDir") String parentDir,
      @JsonProperty("name") String name,
      @JsonProperty("targetPath") String targetPath,
      @JsonProperty("content") String content,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.parentDir = parentDir;
    this.name = name;
    this.targetPath = targetPath;
    this.content = content;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public TemplateResponse(Template template) {
    this.id = template.getId();
    this.roleId = template.getRoleId();
    this.parentDir = template.getParentDir();
    this.name = template.getName();
    this.targetPath = template.getTargetPath();
    this.content = template.getContent();
    this.createdBy = template.getCreatedBy();
    this.createdAt = template.getCreatedAt();
  }
}
