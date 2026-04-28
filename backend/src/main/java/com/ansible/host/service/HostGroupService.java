package com.ansible.host.service;

import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.project.service.ProjectCleanupService;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HostGroupService {

  private final HostGroupRepository hostGroupRepository;
  private final HostRepository hostRepository;
  private final ProjectAccessChecker accessChecker;
  private final ProjectCleanupService cleanupService;

  @Transactional
  public HostGroupResponse createHostGroup(
      Long projectId, CreateHostGroupRequest request, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    if (hostGroupRepository.existsByProjectIdAndName(projectId, request.getName())) {
      throw new IllegalArgumentException(
          "A host group with this name already exists in the project");
    }
    HostGroup hostGroup = new HostGroup();
    hostGroup.setProjectId(projectId);
    hostGroup.setName(request.getName());
    hostGroup.setDescription(request.getDescription());
    hostGroup.setCreatedBy(currentUserId);
    HostGroup saved = hostGroupRepository.save(hostGroup);
    return new HostGroupResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HostGroupResponse> getHostGroupsByProject(Long projectId, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    return hostGroupRepository.findAllByProjectId(projectId).stream()
        .map(HostGroupResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HostGroupResponse getHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return new HostGroupResponse(hostGroup);
  }

  @Transactional
  public HostGroupResponse updateHostGroup(
      Long hostGroupId, UpdateHostGroupRequest request, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(
        hostGroup.getProjectId(), hostGroup.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())
        && !request.getName().equals(hostGroup.getName())) {
      if (hostGroupRepository.existsByProjectIdAndName(
          hostGroup.getProjectId(), request.getName())) {
        throw new IllegalArgumentException(
            "A host group with this name already exists in the project");
      }
      hostGroup.setName(request.getName());
    }
    if (request.getDescription() != null) {
      hostGroup.setDescription(request.getDescription());
    }
    HostGroup saved = hostGroupRepository.save(hostGroup);
    return new HostGroupResponse(saved);
  }

  @Transactional
  public HostGroupResponse copyHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup source =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(
        source.getProjectId(), source.getCreatedBy(), currentUserId);

    String newName = source.getName() + " (副本)";
    if (hostGroupRepository.existsByProjectIdAndName(source.getProjectId(), newName)) {
      int suffix = 2;
      while (hostGroupRepository.existsByProjectIdAndName(
          source.getProjectId(), newName + suffix)) {
        suffix++;
        if (suffix > 100) {
          throw new IllegalStateException(
              "Cannot generate a unique name for the copied host group");
        }
      }
      newName = newName + suffix;
    }

    HostGroup copy = new HostGroup();
    copy.setProjectId(source.getProjectId());
    copy.setName(newName);
    copy.setDescription(source.getDescription());
    copy.setCreatedBy(currentUserId);
    HostGroup saved = hostGroupRepository.save(copy);

    List<Host> sourceHosts = hostRepository.findAllByHostGroupId(hostGroupId);
    if (!sourceHosts.isEmpty()) {
      for (Host sourceHost : sourceHosts) {
        Host hostCopy = new Host();
        hostCopy.setHostGroupId(saved.getId());
        hostCopy.setName(sourceHost.getName());
        hostCopy.setIp(sourceHost.getIp());
        hostCopy.setPort(sourceHost.getPort());
        hostCopy.setAnsibleUser(sourceHost.getAnsibleUser());
        hostCopy.setAnsibleSshPass(sourceHost.getAnsibleSshPass());
        hostCopy.setAnsibleSshPrivateKeyFile(sourceHost.getAnsibleSshPrivateKeyFile());
        hostCopy.setAnsibleBecome(sourceHost.getAnsibleBecome());
        hostCopy.setCreatedBy(currentUserId);
        hostRepository.save(hostCopy);
      }
    }

    return new HostGroupResponse(saved);
  }

  @Transactional
  public void deleteHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(
        hostGroup.getProjectId(), hostGroup.getCreatedBy(), currentUserId);
    cleanupService.cleanupHostGroupResources(hostGroupId);
    hostGroupRepository.delete(hostGroup);
  }
}
