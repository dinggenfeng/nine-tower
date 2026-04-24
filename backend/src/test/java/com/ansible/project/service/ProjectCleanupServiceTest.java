package com.ansible.project.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.environment.entity.Environment;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.playbook.entity.Playbook;
import com.ansible.playbook.repository.PlaybookEnvironmentRepository;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleFileRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import com.ansible.tag.repository.TaskTagRepository;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectCleanupServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskTagRepository taskTagRepository;
  @Mock private HandlerRepository handlerRepository;
  @Mock private TemplateRepository templateRepository;
  @Mock private RoleFileRepository roleFileRepository;
  @Mock private RoleVariableRepository roleVariableRepository;
  @Mock private RoleDefaultVariableRepository roleDefaultVariableRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private HostRepository hostRepository;
  @Mock private VariableRepository variableRepository;
  @Mock private TagRepository tagRepository;
  @Mock private PlaybookRepository playbookRepository;
  @Mock private PlaybookRoleRepository playbookRoleRepository;
  @Mock private PlaybookHostGroupRepository playbookHostGroupRepository;
  @Mock private PlaybookTagRepository playbookTagRepository;
  @Mock private PlaybookEnvironmentRepository playbookEnvironmentRepository;
  @Mock private EnvironmentRepository environmentRepository;
  @Mock private EnvConfigRepository envConfigRepository;

  @InjectMocks private ProjectCleanupService cleanupService;

  @Test
  void cleanupProject_deletesAllChildResources() {
    Long projectId = 1L;

    Playbook pb = new Playbook();
    ReflectionTestUtils.setField(pb, "id", 10L);
    when(playbookRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(pb));

    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 20L);
    when(roleRepository.findAllByProjectId(projectId)).thenReturn(List.of(role));

    HostGroup hg = new HostGroup();
    ReflectionTestUtils.setField(hg, "id", 30L);
    when(hostGroupRepository.findAllByProjectId(projectId)).thenReturn(List.of(hg));

    Environment env = new Environment();
    ReflectionTestUtils.setField(env, "id", 40L);
    when(environmentRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(env));

    Tag tag = new Tag();
    ReflectionTestUtils.setField(tag, "id", 50L);
    when(tagRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(tag));

    cleanupService.cleanupProject(projectId);

    verify(playbookRoleRepository).deleteByPlaybookId(10L);
    verify(playbookTagRepository).deleteByPlaybookId(10L);
    verify(playbookHostGroupRepository).deleteByPlaybookId(10L);
    verify(playbookEnvironmentRepository).deleteByPlaybookId(10L);
    verify(playbookRepository).deleteByProjectId(projectId);

    verify(taskRepository).deleteTaskTagsByRoleId(20L);
    verify(taskRepository).deleteByRoleId(20L);
    verify(handlerRepository).deleteByRoleId(20L);
    verify(templateRepository).deleteByRoleId(20L);
    verify(roleFileRepository).deleteByRoleId(20L);
    verify(roleVariableRepository).deleteByRoleId(20L);
    verify(roleDefaultVariableRepository).deleteByRoleId(20L);
    verify(playbookRoleRepository).deleteByRoleId(20L);
    verify(roleRepository).deleteByProjectId(projectId);

    verify(hostRepository).deleteAllByHostGroupId(30L);
    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.HOSTGROUP, 30L);
    verify(hostGroupRepository).deleteByProjectId(projectId);

    verify(envConfigRepository).deleteByEnvironmentId(40L);
    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, 40L);
    verify(environmentRepository).deleteByProjectId(projectId);

    verify(taskTagRepository).deleteByTagId(50L);
    verify(playbookTagRepository).deleteByTagId(50L);
    verify(tagRepository).deleteByProjectId(projectId);

    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.PROJECT, projectId);
  }

  @Test
  void cleanupRoleResources_deletesAllRoleChildren() {
    cleanupService.cleanupRoleResources(1L);

    verify(taskRepository).deleteTaskTagsByRoleId(1L);
    verify(taskRepository).deleteByRoleId(1L);
    verify(handlerRepository).deleteByRoleId(1L);
    verify(templateRepository).deleteByRoleId(1L);
    verify(roleFileRepository).deleteByRoleId(1L);
    verify(roleVariableRepository).deleteByRoleId(1L);
    verify(roleDefaultVariableRepository).deleteByRoleId(1L);
    verify(playbookRoleRepository).deleteByRoleId(1L);
  }

  @Test
  void cleanupHostGroupResources_deletesHostsAndVariablesAndPlaybookLinks() {
    cleanupService.cleanupHostGroupResources(1L);

    verify(hostRepository).deleteAllByHostGroupId(1L);
    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.HOSTGROUP, 1L);
    verify(playbookHostGroupRepository).deleteByHostGroupId(1L);
  }

  @Test
  void cleanupEnvironmentResources_deletesConfigsAndVariablesAndPlaybookLinks() {
    cleanupService.cleanupEnvironmentResources(1L);

    verify(envConfigRepository).deleteByEnvironmentId(1L);
    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, 1L);
    verify(playbookEnvironmentRepository).deleteByEnvironmentId(1L);
  }

  @Test
  void cleanupTagResources_deletesTaskTagsAndPlaybookTags() {
    cleanupService.cleanupTagResources(1L);

    verify(taskTagRepository).deleteByTagId(1L);
    verify(playbookTagRepository).deleteByTagId(1L);
  }
}
