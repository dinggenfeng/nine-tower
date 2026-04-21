package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;
import java.time.LocalDateTime;

public record VariableResponse(
    Long id,
    VariableScope scope,
    Long scopeId,
    String key,
    String value,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
