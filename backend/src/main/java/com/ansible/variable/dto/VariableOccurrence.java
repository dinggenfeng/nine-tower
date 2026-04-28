package com.ansible.variable.dto;

public record VariableOccurrence(
    Long roleId,
    String roleName,
    String type,       // "TASK", "HANDLER", "TEMPLATE"
    Long entityId,
    String entityName,
    String field       // "args", "whenCondition", "loop", "content", "name"
) {}
