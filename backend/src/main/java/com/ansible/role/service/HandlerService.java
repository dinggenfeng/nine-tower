package com.ansible.role.service;

import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HandlerService {

  private final HandlerRepository handlerRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public HandlerResponse createHandler(
      Long roleId, CreateHandlerRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    Handler handler = new Handler();
    handler.setRoleId(roleId);
    handler.setName(request.getName());
    handler.setModule(request.getModule());
    handler.setArgs(request.getArgs());
    handler.setWhenCondition(request.getWhenCondition());
    handler.setRegister(request.getRegister());
    handler.setCreatedBy(currentUserId);
    Handler saved = handlerRepository.save(handler);
    return new HandlerResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HandlerResponse> getHandlersByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return handlerRepository.findAllByRoleId(roleId).stream()
        .map(HandlerResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HandlerResponse getHandler(Long handlerId, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new HandlerResponse(handler);
  }

  @Transactional
  public HandlerResponse updateHandler(
      Long handlerId, UpdateHandlerRequest request, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), handler.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getName())) {
      handler.setName(request.getName());
    }
    if (StringUtils.hasText(request.getModule())) {
      handler.setModule(request.getModule());
    }
    if (request.getArgs() != null) {
      handler.setArgs(request.getArgs());
    }
    if (request.getWhenCondition() != null) {
      handler.setWhenCondition(request.getWhenCondition());
    }
    if (request.getRegister() != null) {
      handler.setRegister(request.getRegister());
    }
    Handler saved = handlerRepository.save(handler);
    return new HandlerResponse(saved);
  }

  @Transactional
  public void deleteHandler(Long handlerId, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), handler.getCreatedBy(), currentUserId);
    handlerRepository.delete(handler);
  }
}
