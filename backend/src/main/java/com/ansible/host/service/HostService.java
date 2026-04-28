package com.ansible.host.service;

import com.ansible.common.EncryptionService;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HostService {

  private final HostRepository hostRepository;
  private final HostGroupRepository hostGroupRepository;
  private final ProjectAccessChecker accessChecker;
  private final EncryptionService encryptionService;

  @Transactional
  public HostResponse createHost(Long hostGroupId, CreateHostRequest request, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    Host host = new Host();
    host.setHostGroupId(hostGroupId);
    host.setName(request.getName());
    host.setIp(request.getIp());
    host.setPort(Objects.requireNonNullElse(request.getPort(), 22));
    host.setAnsibleUser(request.getAnsibleUser());

    Host sourceHost = null;
    if (request.getCopyFromHostId() != null) {
      sourceHost = hostRepository.findById(request.getCopyFromHostId()).orElse(null);
    }

    if (StringUtils.hasText(request.getAnsibleSshPass())) {
      host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
    } else if (sourceHost != null && sourceHost.getAnsibleSshPass() != null) {
      host.setAnsibleSshPass(sourceHost.getAnsibleSshPass());
    }
    if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())) {
      host.setAnsibleSshPrivateKeyFile(
          encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
    } else if (sourceHost != null && sourceHost.getAnsibleSshPrivateKeyFile() != null) {
      host.setAnsibleSshPrivateKeyFile(sourceHost.getAnsibleSshPrivateKeyFile());
    }
    host.setAnsibleBecome(Objects.requireNonNullElse(request.getAnsibleBecome(), false));
    host.setCreatedBy(currentUserId);
    Host saved = hostRepository.save(host);
    return new HostResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HostResponse> getHostsByHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return hostRepository.findAllByHostGroupId(hostGroupId).stream()
        .map(HostResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HostResponse getHost(Long hostId, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return new HostResponse(host);
  }

  @Transactional
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
  public HostResponse updateHost(Long hostId, UpdateHostRequest request, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), host.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())) {
      host.setName(request.getName());
    }
    if (StringUtils.hasText(request.getIp())) {
      host.setIp(request.getIp());
    }
    if (request.getPort() != null) {
      host.setPort(request.getPort());
    }
    if (request.getAnsibleUser() != null) {
      host.setAnsibleUser(request.getAnsibleUser());
    }
    if (StringUtils.hasText(request.getAnsibleSshPass())
        && !EncryptionService.mask().equals(request.getAnsibleSshPass())) {
      host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
    }
    if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())
        && !EncryptionService.mask().equals(request.getAnsibleSshPrivateKeyFile())) {
      host.setAnsibleSshPrivateKeyFile(
          encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
    }
    if (request.getAnsibleBecome() != null) {
      host.setAnsibleBecome(request.getAnsibleBecome());
    }
    Host saved = hostRepository.save(host);
    return new HostResponse(saved);
  }

  @Transactional
  public void deleteHost(Long hostId, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), host.getCreatedBy(), currentUserId);
    hostRepository.delete(host);
  }
}