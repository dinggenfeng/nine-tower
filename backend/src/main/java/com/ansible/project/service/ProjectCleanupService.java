package com.ansible.project.service;

import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.playbook.repository.PlaybookEnvironmentRepository;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleFileRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.tag.repository.TagRepository;
import com.ansible.tag.repository.TaskTagRepository;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCleanupService {

  private final RoleRepository roleRepository;
  private final TaskRepository taskRepository;
  private final TaskTagRepository taskTagRepository;
  private final HandlerRepository handlerRepository;
  private final TemplateRepository templateRepository;
  private final RoleFileRepository roleFileRepository;
  private final HostGroupRepository hostGroupRepository;
  private final HostRepository hostRepository;
  private final VariableRepository variableRepository;
  private final TagRepository tagRepository;
  private final PlaybookRepository playbookRepository;
  private final PlaybookRoleRepository playbookRoleRepository;
  private final PlaybookHostGroupRepository playbookHostGroupRepository;
  private final PlaybookTagRepository playbookTagRepository;
  private final PlaybookEnvironmentRepository playbookEnvironmentRepository;
  private final EnvironmentRepository environmentRepository;
  private final EnvConfigRepository envConfigRepository;

  @Transactional
  public void cleanupProject(Long projectId) {
    List<Long> playbookIds =
        playbookRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(p -> p.getId())
            .toList();
    for (Long pbId : playbookIds) {
      playbookRoleRepository.deleteByPlaybookId(pbId);
      playbookHostGroupRepository.deleteByPlaybookId(pbId);
      playbookTagRepository.deleteByPlaybookId(pbId);
      playbookEnvironmentRepository.deleteByPlaybookId(pbId);
    }
    playbookRepository.deleteByProjectId(projectId);

    List<Long> roleIds =
        roleRepository.findAllByProjectId(projectId).stream()
            .map(r -> r.getId())
            .toList();
    for (Long roleId : roleIds) {
      cleanupRoleResources(roleId);
    }
    roleRepository.deleteByProjectId(projectId);

    List<Long> hostGroupIds =
        hostGroupRepository.findAllByProjectId(projectId).stream()
            .map(hg -> hg.getId())
            .toList();
    for (Long hgId : hostGroupIds) {
      hostRepository.deleteAllByHostGroupId(hgId);
      variableRepository.deleteByScopeAndScopeId(VariableScope.HOSTGROUP, hgId);
    }
    hostGroupRepository.deleteByProjectId(projectId);

    List<Long> envIds =
        environmentRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(e -> e.getId())
            .toList();
    for (Long envId : envIds) {
      envConfigRepository.deleteByEnvironmentId(envId);
      variableRepository.deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, envId);
    }
    environmentRepository.deleteByProjectId(projectId);

    List<Long> tagIds =
        tagRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(t -> t.getId())
            .toList();
    for (Long tagId : tagIds) {
      taskTagRepository.deleteByTagId(tagId);
      playbookTagRepository.deleteByTagId(tagId);
    }
    tagRepository.deleteByProjectId(projectId);

    variableRepository.deleteByScopeAndScopeId(VariableScope.PROJECT, projectId);
  }

  @Transactional
  public void cleanupRoleResources(Long roleId) {
    taskRepository.deleteTaskTagsByRoleId(roleId);
    taskRepository.deleteByRoleId(roleId);
    handlerRepository.deleteByRoleId(roleId);
    templateRepository.deleteByRoleId(roleId);
    roleFileRepository.deleteByRoleId(roleId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.ROLE_VARS, roleId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.ROLE_DEFAULTS, roleId);
    playbookRoleRepository.deleteByRoleId(roleId);
  }

  @Transactional
  public void cleanupHostGroupResources(Long hostGroupId) {
    hostRepository.deleteAllByHostGroupId(hostGroupId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.HOSTGROUP, hostGroupId);
    playbookHostGroupRepository.deleteByHostGroupId(hostGroupId);
  }

  @Transactional
  public void cleanupEnvironmentResources(Long envId) {
    envConfigRepository.deleteByEnvironmentId(envId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, envId);
    playbookEnvironmentRepository.deleteByEnvironmentId(envId);
  }

  @Transactional
  public void cleanupTagResources(Long tagId) {
    taskTagRepository.deleteByTagId(tagId);
    playbookTagRepository.deleteByTagId(tagId);
  }
}
