package com.ansible.playbook.yaml;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlaybookYamlGenerator {

  public String generate(
      List<String> roleNames,
      List<String> hostGroupNames,
      List<String> tagNames,
      List<Map.Entry<String, String>> projectVars) {
    StringBuilder sb = new StringBuilder(256);
    sb.append("- hosts: ")
        .append(String.join(",", hostGroupNames))
        .append("\n  become: true\n");

    if (projectVars != null && !projectVars.isEmpty()) {
      sb.append("  vars:\n");
      for (Map.Entry<String, String> entry : projectVars) {
        sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
      }
    }

    if (!roleNames.isEmpty()) {
      sb.append("  roles:\n");
      for (String role : roleNames) {
        sb.append("    - role: ").append(role).append('\n');
      }
    }

    if (!tagNames.isEmpty()) {
      sb.append("  tags: [").append(String.join(", ", tagNames)).append("]\n");
    }

    return sb.toString();
  }

  /**
   * Backward-compatible overload without project variables. Used by tests that do not need vars.
   */
  public String generate(
      List<String> roleNames, List<String> hostGroupNames, List<String> tagNames) {
    return generate(roleNames, hostGroupNames, tagNames, List.of());
  }
}
