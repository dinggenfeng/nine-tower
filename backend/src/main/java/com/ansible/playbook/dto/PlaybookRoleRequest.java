package com.ansible.playbook.dto;

import jakarta.validation.constraints.NotNull;

public record PlaybookRoleRequest(@NotNull Long roleId) { }
