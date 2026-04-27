package com.ansible.role.dto;

import com.ansible.role.entity.Template;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class TemplateResponse {

  private final Long id;
  private final Long roleId;
  private final String parentDir;
  private final String name;
  private final String targetPath;
  private final String content;
  private final Boolean isDirectory;
  private final Long size;
  private final List<TemplateResponse> children;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  @SuppressWarnings("PMD.ExcessiveParameterList")
  public TemplateResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("parentDir") String parentDir,
      @JsonProperty("name") String name,
      @JsonProperty("targetPath") String targetPath,
      @JsonProperty("content") String content,
      @JsonProperty("isDirectory") Boolean isDirectory,
      @JsonProperty("size") Long size,
      @JsonProperty("children") List<TemplateResponse> children,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.parentDir = parentDir;
    this.name = name;
    this.targetPath = targetPath;
    this.content = content;
    this.isDirectory = isDirectory;
    this.size = size;
    this.children = children;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public TemplateResponse(
      String parentDir, String name, List<TemplateResponse> children) {
    this.id = null;
    this.roleId = null;
    this.parentDir = parentDir;
    this.name = name;
    this.targetPath = null;
    this.content = null;
    this.isDirectory = true;
    this.size = null;
    this.children = children;
    this.createdBy = null;
    this.createdAt = null;
  }

  public TemplateResponse(Template template, List<TemplateResponse> children) {
    this.id = template.getId();
    this.roleId = template.getRoleId();
    this.parentDir = template.getParentDir();
    this.name = template.getName();
    this.targetPath = template.getTargetPath();
    this.content = template.getContent();
    this.isDirectory = template.getIsDirectory();
    this.children = children;
    this.createdBy = template.getCreatedBy();
    this.createdAt = template.getCreatedAt();
    if (!Boolean.TRUE.equals(template.getIsDirectory()) && template.getContent() != null) {
      this.size = (long) template.getContent().length();
    } else {
      this.size = null;
    }
  }
}
