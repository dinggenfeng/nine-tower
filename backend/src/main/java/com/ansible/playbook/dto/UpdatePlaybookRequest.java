package com.ansible.playbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePlaybookRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 500) String description,
    String extraVars) { }
