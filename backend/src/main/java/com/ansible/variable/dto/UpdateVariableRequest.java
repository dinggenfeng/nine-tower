package com.ansible.variable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVariableRequest(@NotBlank @Size(max = 200) String key, String value) { }
