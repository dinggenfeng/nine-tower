package com.ansible.playbook.service;

import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookResponse;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.dto.UpdatePlaybookRequest;
import com.ansible.playbook.entity.Playbook;
import com.ansible.playbook.entity.PlaybookHostGroup;
import com.ansible.playbook.entity.PlaybookRole;
import com.ansible.playbook.entity.PlaybookTag;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.playbook.yaml.PlaybookYamlGenerator;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public class PlaybookService {

  private final PlaybookRepository playbookRepository;
  private final PlaybookRoleRepository playbookRoleRepository;
  private final PlaybookHostGroupRepository playbookHostGroupRepository;
  private final PlaybookTagRepository playbookTagRepository;
  private final RoleRepository roleRepository;
  private final HostGroupRepository hostGroupRepository;
  private final TagRepository tagRepository;
  private final PlaybookYamlGenerator yamlGenerator;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public PlaybookResponse createPlaybook(
      Long projectId, CreatePlaybookRequest request, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    Playbook p = new Playbook();
    p.setProjectId(projectId);
    p.setName(request.name());
    p.setDescription(request.description());
    p.setExtraVars(request.extraVars());
    p.setCreatedBy(userId);
    return toResponse(playbookRepository.save(p));
  }

  @Transactional(readOnly = true)
  public List<PlaybookResponse> listPlaybooks(Long projectId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    return playbookRepository.findByProjectIdOrderByIdAsc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public PlaybookResponse getPlaybook(Long playbookId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    return toResponse(p);
  }

  @Transactional
  public PlaybookResponse updatePlaybook(
      Long playbookId, UpdatePlaybookRequest request, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkOwnerOrAdmin(p.getProjectId(), p.getCreatedBy(), userId);
    p.setName(request.name());
    p.setDescription(request.description());
    p.setExtraVars(request.extraVars());
    return toResponse(playbookRepository.save(p));
  }

  @Transactional
  public void deletePlaybook(Long playbookId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkOwnerOrAdmin(p.getProjectId(), p.getCreatedBy(), userId);
    playbookRoleRepository.deleteByPlaybookId(playbookId);
    playbookHostGroupRepository.deleteByPlaybookId(playbookId);
    playbookTagRepository.deleteByPlaybookId(playbookId);
    playbookRepository.delete(p);
  }

  @Transactional
  public void addRole(Long playbookId, PlaybookRoleRequest request, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    if (playbookRoleRepository.existsByPlaybookIdAndRoleId(playbookId, request.roleId())) {
      throw new IllegalArgumentException("Role already added to this playbook");
    }
    List<PlaybookRole> existing =
        playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId);
    PlaybookRole pr = new PlaybookRole();
    pr.setPlaybookId(playbookId);
    pr.setRoleId(request.roleId());
    pr.setOrderIndex(existing.size());
    pr.setCreatedBy(userId);
    playbookRoleRepository.save(pr);
  }

  @Transactional
  public void removeRole(Long playbookId, Long roleId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    playbookRoleRepository.deleteByPlaybookIdAndRoleId(playbookId, roleId);
  }

  @Transactional
  public void reorderRoles(Long playbookId, List<Long> roleIds, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    List<PlaybookRole> existing =
        playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId);
    for (int i = 0; i < roleIds.size(); i++) {
      final int index = i;
      final Long roleId = roleIds.get(i);
      existing.stream()
          .filter(pr -> pr.getRoleId().equals(roleId))
          .findFirst()
          .ifPresent(pr -> pr.setOrderIndex(index));
    }
    playbookRoleRepository.saveAll(existing);
  }

  @Transactional
  public void addHostGroup(Long playbookId, Long hostGroupId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    if (playbookHostGroupRepository.existsByPlaybookIdAndHostGroupId(playbookId, hostGroupId)) {
      throw new IllegalArgumentException("Host group already added");
    }
    PlaybookHostGroup phg = new PlaybookHostGroup();
    phg.setPlaybookId(playbookId);
    phg.setHostGroupId(hostGroupId);
    phg.setCreatedBy(userId);
    playbookHostGroupRepository.save(phg);
  }

  @Transactional
  public void removeHostGroup(Long playbookId, Long hostGroupId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    playbookHostGroupRepository.deleteByPlaybookIdAndHostGroupId(playbookId, hostGroupId);
  }

  @Transactional
  public void addTag(Long playbookId, Long tagId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    if (playbookTagRepository.existsByPlaybookIdAndTagId(playbookId, tagId)) {
      throw new IllegalArgumentException("Tag already added");
    }
    PlaybookTag pt = new PlaybookTag();
    pt.setPlaybookId(playbookId);
    pt.setTagId(tagId);
    pt.setCreatedBy(userId);
    playbookTagRepository.save(pt);
  }

  @Transactional
  public void removeTag(Long playbookId, Long tagId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);
    playbookTagRepository.deleteByPlaybookIdAndTagId(playbookId, tagId);
  }

  @Transactional(readOnly = true)
  public String generateYaml(Long playbookId, Long userId) {
    Playbook p =
        playbookRepository
            .findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
    accessChecker.checkMembership(p.getProjectId(), userId);

    List<String> roleNames =
        playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId).stream()
            .map(
                pr ->
                    roleRepository
                        .findById(pr.getRoleId())
                        .map(Role::getName)
                        .orElse("unknown"))
            .toList();

    List<String> hostGroupNames =
        playbookHostGroupRepository.findByPlaybookId(playbookId).stream()
            .map(
                phg ->
                    hostGroupRepository
                        .findById(phg.getHostGroupId())
                        .map(HostGroup::getName)
                        .orElse("unknown"))
            .toList();

    List<String> tagNames =
        playbookTagRepository.findByPlaybookId(playbookId).stream()
            .map(
                pt ->
                    tagRepository.findById(pt.getTagId()).map(Tag::getName).orElse("unknown"))
            .toList();

    return yamlGenerator.generate(roleNames, hostGroupNames, tagNames);
  }

  private PlaybookResponse toResponse(Playbook p) {
    List<Long> roleIds =
        playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(p.getId()).stream()
            .map(PlaybookRole::getRoleId)
            .toList();
    List<Long> hostGroupIds =
        playbookHostGroupRepository.findByPlaybookId(p.getId()).stream()
            .map(PlaybookHostGroup::getHostGroupId)
            .toList();
    List<Long> tagIds =
        playbookTagRepository.findByPlaybookId(p.getId()).stream()
            .map(PlaybookTag::getTagId)
            .toList();

    return new PlaybookResponse(
        p.getId(),
        p.getProjectId(),
        p.getName(),
        p.getDescription(),
        p.getExtraVars(),
        roleIds,
        hostGroupIds,
        tagIds,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
