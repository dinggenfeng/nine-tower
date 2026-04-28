package com.ansible.variable.dto;

public record BatchVariableSaveRequest(
    String key,
    String saveAs,       // "VARIABLE" or "ROLE_VARIABLE"
    String scope,        // "PROJECT", "HOSTGROUP", "ENVIRONMENT" — only when saveAs=VARIABLE
    Long roleId,         // only when saveAs=ROLE_VARIABLE
    String value
) {}
