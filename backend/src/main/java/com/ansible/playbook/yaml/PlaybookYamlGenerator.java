package com.ansible.playbook.yaml;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaybookYamlGenerator {

  public String generate(
      List<String> roleNames, List<String> hostGroupNames, List<String> tagNames) {
    StringBuilder sb = new StringBuilder(128);
    sb.append("- hosts: ").append(String.join(",", hostGroupNames)).append('\n');

    if (!roleNames.isEmpty()) {
      sb.append("  roles:\n");
      for (String role : roleNames) {
        sb.append("    - ").append(role).append('\n');
      }
    }

    if (!tagNames.isEmpty()) {
      sb.append("  tags: [").append(String.join(", ", tagNames)).append("]\n");
    }

    return sb.toString();
  }
}
