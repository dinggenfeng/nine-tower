package com.ansible.role.dto;

import com.ansible.role.entity.RoleFile;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class FileResponse {

  private final Long id;
  private final Long roleId;
  private final String parentDir;
  private final String name;
  private final Boolean isDirectory;
  private final Long size;
  private final String textContent;
  private final List<FileResponse> children;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  @JsonCreator
  public FileResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("parentDir") String parentDir,
      @JsonProperty("name") String name,
      @JsonProperty("isDirectory") Boolean isDirectory,
      @JsonProperty("size") Long size,
      @JsonProperty("textContent") String textContent,
      @JsonProperty("children") List<FileResponse> children,
      @JsonProperty("createdAt") LocalDateTime createdAt,
      @JsonProperty("updatedAt") LocalDateTime updatedAt) {
    this.id = id;
    this.roleId = roleId;
    this.parentDir = parentDir;
    this.name = name;
    this.isDirectory = isDirectory;
    this.size = size;
    this.textContent = textContent;
    this.children = children;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public FileResponse(RoleFile file, List<FileResponse> children) {
    this.id = file.getId();
    this.roleId = file.getRoleId();
    this.parentDir = file.getParentDir();
    this.name = file.getName();
    this.isDirectory = file.getIsDirectory();
    this.children = children;
    this.createdAt = file.getCreatedAt();
    this.updatedAt = file.getUpdatedAt();
    if (!Boolean.TRUE.equals(file.getIsDirectory()) && file.getContent() != null) {
      this.size = (long) file.getContent().length;
      this.textContent = new String(file.getContent(), java.nio.charset.StandardCharsets.UTF_8);
    } else {
      this.size = null;
      this.textContent = null;
    }
  }
}
