package com.ansible.variable.service;

import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.entity.Template;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.DetectedVariableResponse;
import com.ansible.variable.dto.VariableOccurrence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
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
    if (roles.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, List<VariableOccurrence>> allOccurrences = new LinkedHashMap<>();

    for (Role role : roles) {
      Map<String, List<VariableOccurrence>> roleOccurrences = scanRole(role);
      for (Map.Entry<String, List<VariableOccurrence>> entry :
          roleOccurrences.entrySet()) {
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
          String suggestedScope = distinctRoles.size() == 1 ? "ROLE_VARS" : "PROJECT";
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
      extractVars(task.getName(), roleId, roleName, "TASK", task.getId(),
          task.getName(), "name", map);
      extractVars(task.getArgs(), roleId, roleName, "TASK", task.getId(),
          task.getName(), "args", map);
      extractVars(task.getWhenCondition(), roleId, roleName, "TASK", task.getId(),
          task.getName(), "whenCondition", map);
      extractVars(task.getLoop(), roleId, roleName, "TASK", task.getId(),
          task.getName(), "loop", map);
    }

    for (Handler handler : handlerRepository.findAllByRoleId(roleId)) {
      extractVars(handler.getName(), roleId, roleName, "HANDLER", handler.getId(),
          handler.getName(), "name", map);
      extractVars(handler.getArgs(), roleId, roleName, "HANDLER", handler.getId(),
          handler.getName(), "args", map);
      extractVars(handler.getWhenCondition(), roleId, roleName, "HANDLER", handler.getId(),
          handler.getName(), "whenCondition", map);
    }

    for (Template template : templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId)) {
      if (Boolean.TRUE.equals(template.getIsDirectory())) {
        continue;
      }
      extractVars(template.getContent(), roleId, roleName, "TEMPLATE", template.getId(),
          template.getName(), "content", map);
    }

    return map;
  }

  private void extractVars(String text, Long roleId, String roleName,
      String type, Long entityId, String entityName, String field,
      Map<String, List<VariableOccurrence>> map) {
    if (text == null || text.isBlank()) {
      return;
    }
    Matcher matcher = VAR_PATTERN.matcher(text);
    while (matcher.find()) {
      String varName = matcher.group(1);
      if (isBuiltin(varName)) {
        continue;
      }
      List<VariableOccurrence> occurrences =
          map.computeIfAbsent(varName, k -> new ArrayList<>());
      VariableOccurrence occurrence =
          new VariableOccurrence(roleId, roleName, type, entityId, entityName, field);
      if (!occurrences.contains(occurrence)) {
        occurrences.add(occurrence);
      }
    }
  }

  private boolean isBuiltin(String varName) {
    return "item".equals(varName) || varName.startsWith("item.")
        || varName.startsWith("ansible_");
  }
}
