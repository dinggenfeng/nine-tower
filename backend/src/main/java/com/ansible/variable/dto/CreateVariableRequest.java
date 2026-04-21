package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVariableRequest(
    @NotNull VariableScope scope,
    @NotNull Long scopeId,
    @NotBlank @Size(max = 100) String key,
    String value) {}
