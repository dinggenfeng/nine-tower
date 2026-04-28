package com.ansible.variable.service;

import com.ansible.role.entity.*;
import com.ansible.role.repository.*;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariableDetectionService {

  private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");

  private final RoleRepository roleRepository;
  private final TaskRepository taskRepository;
  private final HandlerRepository handlerRepository;
  private final TemplateRepository templateRepository;
  private final ProjectAccessChecker accessChecker;

  public List<DetectedVariableResponse> detectVariables(Long projectId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    List<Role> roles = roleRepository.findAllByProjectId(projectId);
    if (roles.isEmpty()) return Collections.emptyList();

    Map<String, List<VariableOccurrence>> allOccurrences = new LinkedHashMap<>();

    for (Role role : roles) {
      Map<String, List<VariableOccurrence>> roleOccurrences = scanRole(role);
      for (var entry : roleOccurrences.entrySet()) {
        allOccurrences.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
            .addAll(entry.getValue());
      }
    }

    return allOccurrences.entrySet().stream()
        .map(entry -> {
          String key = entry.getKey();
          List<VariableOccurrence> occurrences = entry.getValue();
          Set<Long> distinctRoles = occurrences.stream()
              .map(VariableOccurrence::roleId).collect(Collectors.toSet());
          String suggestedScope = distinctRoles.size() == 1 ? "ROLE" : "PROJECT";
          return new DetectedVariableResponse(key, occurrences, suggestedScope);
        })
        .sorted(Comparator.comparing(DetectedVariableResponse::key))
        .collect(Collectors.toList());
  }

  private Map<String, List<VariableOccurrence>> scanRole(Role role) {
    Map<String, List<VariableOccurrence>> map = new LinkedHashMap<>();
    Long roleId = role.getId();
    String roleName = role.getName();

    for (Task task : taskRepository.findAllByRoleIdOrderByTaskOrderAsc(roleId)) {
      extractVars(task.getName(), roleId, roleName, "TASK", task.getId(), task.getName(), "name", map);
      extractVars(task.getArgs(), roleId, roleName, "TASK", task.getId(), task.getName(), "args", map);
      extractVars(task.getWhenCondition(), roleId, roleName, "TASK", task.getId(), task.getName(), "whenCondition", map);
      extractVars(task.getLoop(), roleId, roleName, "TASK", task.getId(), task.getName(), "loop", map);
    }

    for (Handler handler : handlerRepository.findAllByRoleId(roleId)) {
      extractVars(handler.getName(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "name", map);
      extractVars(handler.getArgs(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "args", map);
      extractVars(handler.getWhenCondition(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "whenCondition", map);
    }

    for (Template template : templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId)) {
      if (Boolean.TRUE.equals(template.getIsDirectory())) continue;
      extractVars(template.getContent(), roleId, roleName, "TEMPLATE", template.getId(), template.getName(), "content", map);
    }

    return map;
  }

  private void extractVars(String text, Long roleId, String roleName,
      String type, Long entityId, String entityName, String field,
      Map<String, List<VariableOccurrence>> map) {
    if (text == null || text.isBlank()) return;
    Matcher matcher = VAR_PATTERN.matcher(text);
    while (matcher.find()) {
      String varName = matcher.group(1);
      if (isBuiltin(varName)) continue;
      map.computeIfAbsent(varName, k -> new ArrayList<>())
          .add(new VariableOccurrence(roleId, roleName, type, entityId, entityName, field));
    }
  }

  private boolean isBuiltin(String varName) {
    return varName.equals("item") || varName.startsWith("item.")
        || varName.startsWith("ansible_");
  }
}
