package com.ansible.variable.dto;

import java.util.List;

public record DetectedVariableResponse(
    String key,
    List<VariableOccurrence> occurrences,
    String suggestedScope  // "ROLE" or "PROJECT"
) {}
