package com.ansible.playbook.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaybookYamlGeneratorTest {

  private final PlaybookYamlGenerator generator = new PlaybookYamlGenerator();

  @Test
  void generate_emptyPlaybook_returnsHostsAll() {
    String yaml = generator.generate(List.of(), List.of("all"), List.of());
    assertThat(yaml).contains("- hosts: all");
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
    assertThat(yaml).contains("roles:").contains("- nginx").contains("- postgresql");
  }

  @Test
  void generate_fullPlaybook() {
    String yaml =
        generator.generate(List.of("nginx", "app"), List.of("web_servers"), List.of("deploy"));

    assertThat(yaml).contains("- hosts: web_servers");
    assertThat(yaml).contains("roles:");
    assertThat(yaml).contains("- nginx");
    assertThat(yaml).contains("- app");
    assertThat(yaml).contains("tags: [deploy]");
  }
}
