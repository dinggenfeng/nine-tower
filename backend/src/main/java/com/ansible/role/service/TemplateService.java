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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

    boolean isDir = Boolean.TRUE.equals(request.getIsDirectory());
    String parentDir = request.getParentDir() != null ? request.getParentDir() : "";
    if (templateRepository.existsByRoleIdAndParentDirAndName(
        roleId, parentDir, request.getName())) {
      throw new IllegalArgumentException(
          "Template with this name already exists in this directory");
    }

    Template template = new Template();
    template.setRoleId(roleId);
    template.setParentDir(parentDir);
    template.setName(request.getName());
    template.setTargetPath(request.getTargetPath());
    template.setIsDirectory(isDir);
    template.setContent(isDir ? null : request.getContent());
    template.setCreatedBy(currentUserId);
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved, null);
  }

  @Transactional(readOnly = true)
  public List<TemplateResponse> listTemplatesAsTree(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    List<Template> all =
        templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId);
    Map<String, List<Template>> byParent =
        all.stream().collect(Collectors.groupingBy(t -> t.getParentDir() != null ? t.getParentDir() : ""));
    return buildTree("", byParent);
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  private List<TemplateResponse> buildTree(
      String parentDir, Map<String, List<Template>> byParent) {
    List<Template> items = byParent.getOrDefault(parentDir, List.of());
    List<TemplateResponse> result = new java.util.ArrayList<>(
        items.stream()
            .map(
                t ->
                    new TemplateResponse(
                        t,
                        buildTree(
                            t.getParentDir() == null || t.getParentDir().isEmpty()
                                    ? t.getName()
                                    : t.getParentDir() + "/" + t.getName(),
                            byParent)))
            .toList());

    // Add implicit directories: paths in byParent that are children of parentDir
    // but have no corresponding directory entity
    java.util.Set<String> existingNames =
        items.stream().map(Template::getName).collect(Collectors.toSet());
    for (String key : byParent.keySet()) {
      if (key.isEmpty()) {
        continue;
      }
      // key is a direct child of parentDir if parentDir is a prefix and the remainder
      // has no "/"
      String prefix = parentDir.isEmpty() ? "" : parentDir + "/";
      if (key.startsWith(prefix)) {
        String remainder = key.substring(prefix.length());
        int slash = remainder.indexOf('/');
        if (slash > 0) {
          // key like "conf.d/nginx" — the top-level implicit dir is "conf.d"
          remainder = remainder.substring(0, slash);
        }
        if (!remainder.isEmpty() && !existingNames.contains(remainder)) {
          existingNames.add(remainder);
          String childPath = parentDir.isEmpty() ? remainder : parentDir + "/" + remainder;
          result.add(new TemplateResponse(parentDir, remainder, buildTree(childPath, byParent)));
        }
      }
    }

    return result;
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
    return new TemplateResponse(template, null);
  }

  @Transactional
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
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
    if (request.getContent() != null && !Boolean.TRUE.equals(template.getIsDirectory())) {
      template.setContent(request.getContent());
    }
    if (request.getIsDirectory() != null) {
      template.setIsDirectory(request.getIsDirectory());
      if (Boolean.TRUE.equals(request.getIsDirectory())) {
        template.setContent(null);
      }
    }
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved, null);
  }

  @Transactional(readOnly = true)
  public String getTemplateContent(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    if (Boolean.TRUE.equals(template.getIsDirectory())) {
      throw new IllegalArgumentException("Cannot download a directory");
    }
    return template.getContent() != null ? template.getContent() : "";
  }

  @Transactional(readOnly = true)
  public String getTemplateName(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return template.getName();
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
    if (Boolean.TRUE.equals(template.getIsDirectory())) {
      String childPrefix =
          template.getParentDir() == null || template.getParentDir().isEmpty()
              ? template.getName() + "/"
              : template.getParentDir() + "/" + template.getName() + "/";
      List<Template> children =
          templateRepository.findByRoleIdAndParentDirStartingWith(
              template.getRoleId(), childPrefix);
      templateRepository.deleteAll(children);
    }
    templateRepository.delete(template);
  }
}
