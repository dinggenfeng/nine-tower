package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;

public record BatchVariableSaveRequest(
    String key,
    VariableScope scope,
    Long scopeId,
    String value
) {}
