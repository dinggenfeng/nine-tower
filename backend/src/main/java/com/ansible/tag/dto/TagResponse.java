package com.ansible.tag.dto;

import com.ansible.tag.entity.Tag;
import java.time.LocalDateTime;

public record TagResponse(
    Long id, Long projectId, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {

  public TagResponse(Tag tag) {
    this(tag.getId(), tag.getProjectId(), tag.getName(), tag.getCreatedAt(), tag.getUpdatedAt());
  }
}
