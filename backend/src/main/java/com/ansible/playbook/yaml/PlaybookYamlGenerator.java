package com.ansible.playbook.yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlaybookYamlGenerator {

  private static final String INDENT = "    ";
  private static final String SPECIAL_CHARS = ":#'\"\n\t&*!|>?,%@`";

  /**
   * Render a single-play YAML document.
   *
   * @param roleNames role names (in execution order)
   * @param hostGroupNames host group names — joined with comma into the {@code hosts:} field
   * @param tagNames play-level tags
   * @param mergedVars vars block content. Caller is responsible for merging by precedence; the
   *     iteration order of the map is preserved in the output.
   */
  public String generate(
      List<String> roleNames,
      List<String> hostGroupNames,
      List<String> tagNames,
      Map<String, String> mergedVars) {
    StringBuilder sb = new StringBuilder(256);
    String hosts = hostGroupNames.isEmpty() ? "all" : String.join(",", hostGroupNames);
    sb.append("- hosts: ").append(hosts).append("\n  become: true\n");

    if (mergedVars != null && !mergedVars.isEmpty()) {
      sb.append("  vars:\n");
      for (Map.Entry<String, String> entry : mergedVars.entrySet()) {
        sb.append(INDENT)
            .append(entry.getKey())
            .append(": ")
            .append(formatYamlValue(entry.getValue()))
            .append('\n');
      }
    }

    if (!roleNames.isEmpty()) {
      sb.append("  roles:\n");
      for (String role : roleNames) {
        sb.append(INDENT).append("- role: ").append(role).append('\n');
      }
    }

    if (!tagNames.isEmpty()) {
      sb.append("  tags: [").append(String.join(", ", tagNames)).append("]\n");
    }

    return sb.toString();
  }

  /** Convenience overload for callers that don't have any vars to render. */
  public String generate(
      List<String> roleNames, List<String> hostGroupNames, List<String> tagNames) {
    return generate(roleNames, hostGroupNames, tagNames, new LinkedHashMap<>());
  }

  private static String formatYamlValue(String value) {
    if (value == null || value.isEmpty()) {
      return "\"\"";
    }
    if (needsQuoting(value)) {
      return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    return value;
  }

  private static boolean needsQuoting(String value) {
    if (value.isBlank() || !value.equals(value.strip())) {
      return true;
    }
    char first = value.charAt(0);
    if (SPECIAL_CHARS.indexOf(first) >= 0 || first == '-' || first == '[' || first == '{') {
      return true;
    }
    for (int i = 0; i < value.length(); i++) {
      if (SPECIAL_CHARS.indexOf(value.charAt(i)) >= 0) {
        return true;
      }
    }
    return false;
  }
}
