package com.ansible.playbook.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaybookYamlGeneratorTest {

  private final PlaybookYamlGenerator generator = new PlaybookYamlGenerator();

  @Test
  void generate_emptyPlaybook_returnsHostsAll() {
    String yaml = generator.generate(List.of(), List.of("all"), List.of());
    assertThat(yaml).contains("- hosts: all");
    assertThat(yaml).contains("become: true");
  }

  @Test
  void generate_withHostGroups() {
    String yaml = generator.generate(List.of(), List.of("web", "db"), List.of());
    assertThat(yaml).contains("- hosts: web,db");
  }

  @Test
  void generate_withTags() {
    String yaml = generator.generate(List.of(), List.of("all"), List.of("web", "production"));
    assertThat(yaml).contains("tags: [web, production]");
  }

  @Test
  void generate_withRoleNames() {
    String yaml = generator.generate(List.of("nginx", "postgresql"), List.of("all"), List.of());
    assertThat(yaml).contains("roles:").contains("- role: nginx").contains("- role: postgresql");
  }

  @Test
  void generate_fullPlaybook() {
    String yaml =
        generator.generate(List.of("nginx", "app"), List.of("web_servers"), List.of("deploy"));

    assertThat(yaml).contains("- hosts: web_servers");
    assertThat(yaml).contains("become: true");
    assertThat(yaml).contains("roles:");
    assertThat(yaml).contains("- role: nginx");
    assertThat(yaml).contains("- role: app");
    assertThat(yaml).contains("tags: [deploy]");
  }

  @Test
  void generate_withProjectVars() {
    List<Map.Entry<String, String>> vars =
        List.of(
            new AbstractMap.SimpleEntry<>("app_port", "8080"),
            new AbstractMap.SimpleEntry<>("app_name", "myapp"));

    String yaml = generator.generate(List.of("nginx"), List.of("web_servers"), List.of(), vars);

    assertThat(yaml).contains("vars:");
    assertThat(yaml).contains("app_port: 8080");
    assertThat(yaml).contains("app_name: myapp");
  }

  @Test
  void generate_withoutVars_noVarsSection() {
    String yaml = generator.generate(List.of("nginx"), List.of("web_servers"), List.of(), List.of());
    assertThat(yaml).doesNotContain("vars:");
  }
}
