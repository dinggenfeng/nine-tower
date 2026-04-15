package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleDefaultVariableService {

  private final RoleDefaultVariableRepository roleDefaultVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleDefaultVariableResponse createDefault(
      Long roleId, CreateRoleDefaultVariableRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (roleDefaultVariableRepository.existsByRoleIdAndKey(roleId, request.getKey())) {
      throw new IllegalArgumentException("Default variable key already exists in this role");
    }

    RoleDefaultVariable variable = new RoleDefaultVariable();
    variable.setRoleId(roleId);
    variable.setKey(request.getKey());
    variable.setValue(request.getValue());
    variable.setCreatedBy(currentUserId);
    RoleDefaultVariable saved = roleDefaultVariableRepository.save(variable);
    return new RoleDefaultVariableResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleDefaultVariableResponse> getDefaultsByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(roleId).stream()
        .map(RoleDefaultVariableResponse::new)
        .toList();
  }

  @Transactional
  public RoleDefaultVariableResponse updateDefault(
      Long variableId, UpdateRoleDefaultVariableRequest request, Long currentUserId) {
    RoleDefaultVariable variable =
        roleDefaultVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role default variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getKey())) {
      variable.setKey(request.getKey());
    }
    if (request.getValue() != null) {
      variable.setValue(request.getValue());
    }
    RoleDefaultVariable saved = roleDefaultVariableRepository.save(variable);
    return new RoleDefaultVariableResponse(saved);
  }

  @Transactional
  public void deleteDefault(Long variableId, Long currentUserId) {
    RoleDefaultVariable variable =
        roleDefaultVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role default variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);
    roleDefaultVariableRepository.delete(variable);
  }
}
