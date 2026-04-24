package com.ansible.variable.service;

import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VariableService {

  private final VariableRepository variableRepository;
  private final ProjectAccessChecker accessChecker;
  private final HostGroupRepository hostGroupRepository;
  private final EnvironmentRepository environmentRepository;

  private Long resolveProjectId(Variable v) {
    return switch (v.getScope()) {
      case PROJECT -> v.getScopeId();
      case HOSTGROUP -> hostGroupRepository
          .findById(v.getScopeId())
          .orElseThrow(() -> new IllegalArgumentException("Host group not found"))
          .getProjectId();
      case ENVIRONMENT -> environmentRepository
          .findById(v.getScopeId())
          .orElseThrow(() -> new IllegalArgumentException("Environment not found"))
          .getProjectId();
    };
  }

  @Transactional
  public VariableResponse createVariable(
      Long projectId, CreateVariableRequest request, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    if (variableRepository.existsByScopeAndScopeIdAndKey(
        request.scope(), request.scopeId(), request.key())) {
      throw new IllegalArgumentException(
          "Variable '" + request.key() + "' already exists in this scope");
    }
    Variable v = new Variable();
    v.setScope(request.scope());
    v.setScopeId(request.scopeId());
    v.setKey(request.key());
    v.setValue(request.value());
    v.setCreatedBy(userId);
    return toResponse(variableRepository.save(v));
  }

  @Transactional(readOnly = true)
  public List<VariableResponse> listVariables(
      Long projectId, VariableScope scope, Long scopeId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    if (scopeId != null) {
      return variableRepository.findByScopeAndScopeIdOrderByIdAsc(scope, scopeId).stream()
          .map(this::toResponse)
          .toList();
    }
    return variableRepository.findByScopeOrderByIdAsc(scope).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public VariableResponse getVariable(Long varId, Long userId) {
    Variable v =
        variableRepository
            .findById(varId)
            .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
    accessChecker.checkMembership(resolveProjectId(v), userId);
    return toResponse(v);
  }

  @Transactional
  public VariableResponse updateVariable(
      Long varId, UpdateVariableRequest request, Long userId) {
    Variable v =
        variableRepository
            .findById(varId)
            .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
    accessChecker.checkOwnerOrAdmin(resolveProjectId(v), v.getCreatedBy(), userId);
    if (!v.getKey().equals(request.key())
        && variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(
            v.getScope(), v.getScopeId(), request.key(), varId)) {
      throw new IllegalArgumentException(
          "Variable '" + request.key() + "' already exists in this scope");
    }
    v.setKey(request.key());
    v.setValue(request.value());
    return toResponse(variableRepository.save(v));
  }

  @Transactional
  public void deleteVariable(Long varId, Long userId) {
    Variable v =
        variableRepository
            .findById(varId)
            .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
    accessChecker.checkOwnerOrAdmin(resolveProjectId(v), v.getCreatedBy(), userId);
    variableRepository.delete(v);
  }

  private VariableResponse toResponse(Variable v) {
    return new VariableResponse(
        v.getId(),
        v.getScope(),
        v.getScopeId(),
        v.getKey(),
        v.getValue(),
        v.getCreatedAt(),
        v.getUpdatedAt());
  }
}
