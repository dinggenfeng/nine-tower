package com.ansible.playbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.environment.entity.EnvConfig;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookResponse;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.entity.Playbook;
import com.ansible.playbook.entity.PlaybookEnvironment;
import com.ansible.playbook.entity.PlaybookHostGroup;
import com.ansible.playbook.entity.PlaybookRole;
import com.ansible.playbook.entity.PlaybookTag;
import com.ansible.playbook.repository.PlaybookEnvironmentRepository;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.playbook.yaml.PlaybookYamlGenerator;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaybookServiceTest {

  @Mock private PlaybookRepository playbookRepository;
  @Mock private PlaybookRoleRepository playbookRoleRepository;
  @Mock private PlaybookHostGroupRepository playbookHostGroupRepository;
  @Mock private PlaybookTagRepository playbookTagRepository;
  @Mock private PlaybookEnvironmentRepository playbookEnvironmentRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private RoleVariableRepository roleVariableRepository;
  @Mock private RoleDefaultVariableRepository roleDefaultVariableRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private TagRepository tagRepository;
  @Mock private VariableRepository variableRepository;
  @Mock private EnvConfigRepository envConfigRepository;
  @Mock private PlaybookYamlGenerator yamlGenerator;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private PlaybookService playbookService;

  @Test
  void createPlaybook_success() {
    Playbook saved = new Playbook();
    ReflectionTestUtils.setField(saved, "id", 1L);
    saved.setProjectId(1L);
    saved.setName("deploy.yml");
    saved.setCreatedBy(100L);
    when(playbookRepository.save(any(Playbook.class))).thenReturn(saved);

    PlaybookResponse response =
        playbookService.createPlaybook(
            1L, new CreatePlaybookRequest("deploy.yml", "Deploy app", null), 100L);

    assertThat(response.name()).isEqualTo("deploy.yml");
  }

  @Test
  void listPlaybooks_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    p.setName("deploy.yml");
    p.setCreatedBy(100L);
    when(playbookRepository.findByProjectIdOrderByIdAsc(1L)).thenReturn(List.of(p));
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookEnvironmentRepository.findByPlaybookId(1L)).thenReturn(List.of());

    List<PlaybookResponse> list = playbookService.listPlaybooks(1L, 100L);
    assertThat(list).hasSize(1);
  }

  @Test
  void addRole_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(false);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    PlaybookRole savedRole = new PlaybookRole();
    ReflectionTestUtils.setField(savedRole, "id", 100L);
    when(playbookRoleRepository.save(any(PlaybookRole.class))).thenReturn(savedRole);

    playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L);

    verify(playbookRoleRepository).save(any(PlaybookRole.class));
  }

  @Test
  void addRole_duplicate_throws() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(true);

    assertThatThrownBy(() -> playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already added");
  }

  @Test
  void removeRole_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    playbookService.removeRole(1L, 10L, 100L);
    verify(playbookRoleRepository).deleteByPlaybookIdAndRoleId(1L, 10L);
  }

  @Test
  void addHostGroup_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookHostGroupRepository.existsByPlaybookIdAndHostGroupId(1L, 5L)).thenReturn(false);
    PlaybookHostGroup savedHg = new PlaybookHostGroup();
    ReflectionTestUtils.setField(savedHg, "id", 100L);
    when(playbookHostGroupRepository.save(any(PlaybookHostGroup.class))).thenReturn(savedHg);

    playbookService.addHostGroup(1L, 5L, 100L);
    verify(playbookHostGroupRepository).save(any(PlaybookHostGroup.class));
  }

  @Test
  void generateYaml_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    PlaybookRole pr = new PlaybookRole();
    pr.setRoleId(10L);
    pr.setOrderIndex(0);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(pr));

    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 10L);
    role.setName("nginx");
    when(roleRepository.findById(10L)).thenReturn(Optional.of(role));

    PlaybookHostGroup phg = new PlaybookHostGroup();
    phg.setHostGroupId(5L);
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of(phg));

    HostGroup hg = new HostGroup();
    ReflectionTestUtils.setField(hg, "id", 5L);
    hg.setName("web_servers");
    when(hostGroupRepository.findById(5L)).thenReturn(Optional.of(hg));

    PlaybookTag pt = new PlaybookTag();
    pt.setTagId(3L);
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of(pt));

    Tag tag = new Tag();
    ReflectionTestUtils.setField(tag, "id", 3L);
    tag.setName("deploy");
    when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));

    when(playbookEnvironmentRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(roleVariableRepository.findAllByRoleIdOrderByKeyAsc(10L)).thenReturn(List.of());
    when(roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(10L)).thenReturn(List.of());
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 1L))
        .thenReturn(List.of());
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.HOSTGROUP, 5L))
        .thenReturn(List.of());

    when(yamlGenerator.generate(
            eq(List.of("nginx")),
            eq(List.of("web_servers")),
            eq(List.of("deploy")),
            any(Map.class)))
        .thenReturn("- hosts: web_servers\n  roles:\n    - nginx\n  tags: [deploy]\n");

    String yaml = playbookService.generateYaml(1L, 100L);

    assertThat(yaml).contains("nginx", "web_servers", "deploy");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateYaml_mergesVarsByPriority() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    p.setExtraVars("override_me: from_extra\nfrom_extra_only: x");
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    PlaybookRole pr = new PlaybookRole();
    pr.setRoleId(10L);
    pr.setOrderIndex(0);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(pr));
    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 10L);
    role.setName("nginx");
    when(roleRepository.findById(10L)).thenReturn(Optional.of(role));

    PlaybookHostGroup phg = new PlaybookHostGroup();
    phg.setHostGroupId(5L);
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of(phg));
    HostGroup hg = new HostGroup();
    ReflectionTestUtils.setField(hg, "id", 5L);
    hg.setName("web");
    when(hostGroupRepository.findById(5L)).thenReturn(Optional.of(hg));

    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());

    PlaybookEnvironment pe = new PlaybookEnvironment();
    pe.setEnvironmentId(7L);
    when(playbookEnvironmentRepository.findByPlaybookId(1L)).thenReturn(List.of(pe));

    // Same key "override_me" appears at every level; the last write wins.
    RoleDefaultVariable defaultVar = new RoleDefaultVariable();
    defaultVar.setKey("override_me");
    defaultVar.setValue("from_default");
    defaultVar.setRoleId(10L);
    RoleDefaultVariable defaultOnly = new RoleDefaultVariable();
    defaultOnly.setKey("from_default_only");
    defaultOnly.setValue("d");
    defaultOnly.setRoleId(10L);
    when(roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(10L))
        .thenReturn(List.of(defaultVar, defaultOnly));

    RoleVariable roleVar = new RoleVariable();
    roleVar.setKey("override_me");
    roleVar.setValue("from_role_var");
    roleVar.setRoleId(10L);
    when(roleVariableRepository.findAllByRoleIdOrderByKeyAsc(10L)).thenReturn(List.of(roleVar));

    Variable projectVar = new Variable();
    projectVar.setKey("override_me");
    projectVar.setValue("from_project");
    projectVar.setScope(VariableScope.PROJECT);
    projectVar.setScopeId(1L);
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 1L))
        .thenReturn(List.of(projectVar));

    Variable hgVar = new Variable();
    hgVar.setKey("override_me");
    hgVar.setValue("from_hostgroup");
    hgVar.setScope(VariableScope.HOSTGROUP);
    hgVar.setScopeId(5L);
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.HOSTGROUP, 5L))
        .thenReturn(List.of(hgVar));

    EnvConfig envConfig = new EnvConfig();
    envConfig.setConfigKey("override_me");
    envConfig.setConfigValue("from_envconfig");
    envConfig.setEnvironmentId(7L);
    when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(7L))
        .thenReturn(List.of(envConfig));

    Variable envVar = new Variable();
    envVar.setKey("override_me");
    envVar.setValue("from_env_var");
    envVar.setScope(VariableScope.ENVIRONMENT);
    envVar.setScopeId(7L);
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.ENVIRONMENT, 7L))
        .thenReturn(List.of(envVar));

    when(yamlGenerator.generate(any(), any(), any(), any(Map.class))).thenReturn("yaml");

    playbookService.generateYaml(1L, 100L);

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(yamlGenerator).generate(any(), any(), any(), captor.capture());
    Map<String, String> mergedVars = captor.getValue();

    // extraVars wins over everything
    assertThat(mergedVars).containsEntry("override_me", "from_extra");
    assertThat(mergedVars).containsEntry("from_extra_only", "x");
    assertThat(mergedVars).containsEntry("from_default_only", "d");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateYaml_envConfigOverridesProjectAndRoleVars() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    PlaybookRole pr = new PlaybookRole();
    pr.setRoleId(10L);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(pr));
    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 10L);
    role.setName("nginx");
    when(roleRepository.findById(10L)).thenReturn(Optional.of(role));

    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());

    PlaybookEnvironment pe = new PlaybookEnvironment();
    pe.setEnvironmentId(7L);
    when(playbookEnvironmentRepository.findByPlaybookId(1L)).thenReturn(List.of(pe));

    RoleVariable roleVar = new RoleVariable();
    roleVar.setKey("port");
    roleVar.setValue("80");
    roleVar.setRoleId(10L);
    when(roleVariableRepository.findAllByRoleIdOrderByKeyAsc(10L)).thenReturn(List.of(roleVar));
    when(roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(10L)).thenReturn(List.of());

    Variable projectVar = new Variable();
    projectVar.setKey("port");
    projectVar.setValue("8080");
    projectVar.setScope(VariableScope.PROJECT);
    projectVar.setScopeId(1L);
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 1L))
        .thenReturn(List.of(projectVar));

    EnvConfig envConfig = new EnvConfig();
    envConfig.setConfigKey("port");
    envConfig.setConfigValue("443");
    envConfig.setEnvironmentId(7L);
    when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(7L))
        .thenReturn(List.of(envConfig));
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.ENVIRONMENT, 7L))
        .thenReturn(List.of());

    when(yamlGenerator.generate(any(), any(), any(), any(Map.class))).thenReturn("yaml");

    playbookService.generateYaml(1L, 100L);

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(yamlGenerator).generate(any(), any(), any(), captor.capture());
    assertThat(captor.getValue()).containsEntry("port", "443");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateYaml_invalidExtraVars_isIgnored() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    p.setExtraVars("not a yaml mapping: [unbalanced");
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookEnvironmentRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 1L))
        .thenReturn(List.of());
    when(yamlGenerator.generate(any(), any(), any(), any(Map.class))).thenReturn("yaml");

    playbookService.generateYaml(1L, 100L);

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(yamlGenerator).generate(any(), any(), any(), captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  void deletePlaybook_cascades() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    playbookService.deletePlaybook(1L, 100L);

    verify(playbookRoleRepository).deleteByPlaybookId(1L);
    verify(playbookHostGroupRepository).deleteByPlaybookId(1L);
    verify(playbookTagRepository).deleteByPlaybookId(1L);
    verify(playbookEnvironmentRepository).deleteByPlaybookId(1L);
    verify(playbookRepository).delete(p);
  }

  @Test
  void addEnvironment_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookEnvironmentRepository.existsByPlaybookIdAndEnvironmentId(1L, 7L))
        .thenReturn(false);
    PlaybookEnvironment savedEnv = new PlaybookEnvironment();
    ReflectionTestUtils.setField(savedEnv, "id", 100L);
    when(playbookEnvironmentRepository.save(any(PlaybookEnvironment.class)))
        .thenReturn(savedEnv);

    playbookService.addEnvironment(1L, 7L, 100L);
    verify(playbookEnvironmentRepository).save(any(PlaybookEnvironment.class));
  }

  @Test
  void addEnvironment_duplicate_throws() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookEnvironmentRepository.existsByPlaybookIdAndEnvironmentId(1L, 7L))
        .thenReturn(true);

    assertThatThrownBy(() -> playbookService.addEnvironment(1L, 7L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already added");
  }

  @Test
  void removeEnvironment_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    playbookService.removeEnvironment(1L, 7L, 100L);
    verify(playbookEnvironmentRepository).deleteByPlaybookIdAndEnvironmentId(1L, 7L);
  }
}
