package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleVariableService {

  private final RoleVariableRepository roleVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleVariableResponse createVariable(
      Long roleId, CreateRoleVariableRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (roleVariableRepository.existsByRoleIdAndKey(roleId, request.getKey())) {
      throw new IllegalArgumentException("Variable key already exists in this role");
    }

    RoleVariable variable = new RoleVariable();
    variable.setRoleId(roleId);
    variable.setKey(request.getKey());
    variable.setValue(request.getValue());
    variable.setCreatedBy(currentUserId);
    RoleVariable saved = roleVariableRepository.save(variable);
    return new RoleVariableResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleVariableResponse> getVariablesByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return roleVariableRepository.findAllByRoleIdOrderByKeyAsc(roleId).stream()
        .map(RoleVariableResponse::new)
        .toList();
  }

  @Transactional
  public RoleVariableResponse updateVariable(
      Long variableId, UpdateRoleVariableRequest request, Long currentUserId) {
    RoleVariable variable =
        roleVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role variable not found"));
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
    RoleVariable saved = roleVariableRepository.save(variable);
    return new RoleVariableResponse(saved);
  }

  @Transactional
  public void deleteVariable(Long variableId, Long currentUserId) {
    RoleVariable variable =
        roleVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);
    roleVariableRepository.delete(variable);
  }
}
