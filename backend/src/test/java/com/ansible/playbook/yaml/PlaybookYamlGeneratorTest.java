package com.ansible.playbook.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
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
  void generate_noHostGroups_defaultsToAll() {
    String yaml = generator.generate(List.of(), List.of(), List.of());
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
  void generate_withVars_preservesInsertionOrder() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("app_port", "8080");
    vars.put("app_name", "myapp");

    String yaml = generator.generate(List.of("nginx"), List.of("web_servers"), List.of(), vars);

    assertThat(yaml).contains("vars:");
    int portIdx = yaml.indexOf("app_port: 8080");
    int nameIdx = yaml.indexOf("app_name: myapp");
    assertThat(portIdx).isPositive();
    assertThat(nameIdx).isGreaterThan(portIdx);
  }

  @Test
  void generate_withoutVars_noVarsSection() {
    String yaml = generator.generate(List.of("nginx"), List.of("web_servers"), List.of(), Map.of());
    assertThat(yaml).doesNotContain("vars:");
  }

  @Test
  void generate_nullVars_noVarsSection() {
    String yaml = generator.generate(List.of("nginx"), List.of("web_servers"), List.of(), null);
    assertThat(yaml).doesNotContain("vars:");
  }

  @Test
  void generate_quotesValuesWithSpecialChars() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("url", "http://example.com:8080/path");
    vars.put("greeting", "hello # world");
    vars.put("plain", "simple");

    String yaml = generator.generate(List.of(), List.of("all"), List.of(), vars);

    assertThat(yaml).contains("url: \"http://example.com:8080/path\"");
    assertThat(yaml).contains("greeting: \"hello # world\"");
    assertThat(yaml).contains("plain: simple");
  }

  @Test
  void generate_quotesEmptyValue() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("blank", "");
    vars.put("nullish", null);

    String yaml = generator.generate(List.of(), List.of("all"), List.of(), vars);

    assertThat(yaml).contains("blank: \"\"");
    assertThat(yaml).contains("nullish: \"\"");
  }

  @Test
  void generate_escapesQuotesAndBackslashes() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("path", "C:\\Users\\dev");
    vars.put("quoted", "say \"hi\"");

    String yaml = generator.generate(List.of(), List.of("all"), List.of(), vars);

    assertThat(yaml).contains("path: \"C:\\\\Users\\\\dev\"");
    assertThat(yaml).contains("quoted: \"say \\\"hi\\\"\"");
  }

  @Test
  void generate_quotesValueStartingWithSpecialChar() {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("dash", "-leading-dash");
    vars.put("brace", "{not a flow map}");

    String yaml = generator.generate(List.of(), List.of("all"), List.of(), vars);

    assertThat(yaml).contains("dash: \"-leading-dash\"");
    assertThat(yaml).contains("brace: \"{not a flow map}\"");
  }
}
