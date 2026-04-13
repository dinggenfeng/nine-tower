package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleService {

  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleResponse createRole(Long projectId, CreateRoleRequest request, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    Role role = new Role();
    role.setProjectId(projectId);
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    role.setCreatedBy(currentUserId);
    Role saved = roleRepository.save(role);
    return new RoleResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleResponse> getRolesByProject(Long projectId, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    return roleRepository.findAllByProjectId(projectId).stream()
        .map(RoleResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public RoleResponse getRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new RoleResponse(role);
  }

  @Transactional
  public RoleResponse updateRole(Long roleId, UpdateRoleRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), role.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())) {
      role.setName(request.getName());
    }
    if (request.getDescription() != null) {
      role.setDescription(request.getDescription());
    }
    Role saved = roleRepository.save(role);
    return new RoleResponse(saved);
  }

  @Transactional
  public void deleteRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), role.getCreatedBy(), currentUserId);
    roleRepository.delete(role);
  }
}
