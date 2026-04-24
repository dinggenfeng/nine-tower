package com.ansible.playbook.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PlaybookResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    String extraVars,
    List<Long> roleIds,
    List<Long> hostGroupIds,
    List<Long> tagIds,
    List<Long> environmentIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) { }
