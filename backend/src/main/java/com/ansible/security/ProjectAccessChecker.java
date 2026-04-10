package com.ansible.security;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessChecker {

  private final ProjectMemberRepository projectMemberRepository;

  public ProjectMember checkMembership(Long projectId, Long userId) {
    return projectMemberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new SecurityException("Not a member of this project"));
  }

  public void checkAdmin(Long projectId, Long userId) {
    ProjectMember member = checkMembership(projectId, userId);
    if (member.getRole() != ProjectRole.PROJECT_ADMIN) {
      throw new SecurityException("Only project admins can perform this action");
    }
  }
}
