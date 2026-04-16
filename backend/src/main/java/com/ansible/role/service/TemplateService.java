package com.ansible.role.service;

import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Template;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TemplateService {

  private final TemplateRepository templateRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public TemplateResponse createTemplate(
      Long roleId, CreateTemplateRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (templateRepository.existsByRoleIdAndParentDirAndName(
        roleId, request.getParentDir(), request.getName())) {
      throw new IllegalArgumentException(
          "Template with this name already exists in this directory");
    }

    Template template = new Template();
    template.setRoleId(roleId);
    template.setParentDir(request.getParentDir());
    template.setName(request.getName());
    template.setTargetPath(request.getTargetPath());
    template.setContent(request.getContent());
    template.setCreatedBy(currentUserId);
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<TemplateResponse> getTemplatesByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId).stream()
        .map(TemplateResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public TemplateResponse getTemplate(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new TemplateResponse(template);
  }

  @Transactional
  public TemplateResponse updateTemplate(
      Long templateId, UpdateTemplateRequest request, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), template.getCreatedBy(), currentUserId);

    String newParentDir = request.getParentDir();
    String newName = request.getName();
    boolean parentDirChanged =
        newParentDir != null && !Objects.equals(newParentDir, template.getParentDir());
    boolean nameChanged =
        StringUtils.hasText(newName) && !newName.equals(template.getName());

    if (parentDirChanged || nameChanged) {
      String checkDir = parentDirChanged ? newParentDir : template.getParentDir();
      String checkName = nameChanged ? newName : template.getName();
      if (templateRepository.existsByRoleIdAndParentDirAndName(
          template.getRoleId(), checkDir, checkName)) {
        throw new IllegalArgumentException(
            "Template with this name already exists in this directory");
      }
      if (parentDirChanged) {
        template.setParentDir(newParentDir);
      }
      if (nameChanged) {
        template.setName(newName);
      }
    }

    if (request.getTargetPath() != null) {
      template.setTargetPath(request.getTargetPath());
    }
    if (request.getContent() != null) {
      template.setContent(request.getContent());
    }
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved);
  }

  @Transactional
  public void deleteTemplate(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), template.getCreatedBy(), currentUserId);
    templateRepository.delete(template);
  }
}
