package com.ansible.project.service;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional(readOnly = true)
  public List<ProjectMemberResponse> listMembers(Long projectId, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);
    return members.stream()
        .map(
            member -> {
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(() -> new IllegalArgumentException("User not found"));
              return new ProjectMemberResponse(member, user);
            })
        .toList();
  }

  @Transactional
  public ProjectMemberResponse addMember(
      Long projectId, AddMemberRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
      throw new IllegalArgumentException("User is already a member of this project");
    }
    ProjectMember member = new ProjectMember();
    member.setProjectId(projectId);
    member.setUserId(request.getUserId());
    member.setRole(request.getRole());
    ProjectMember saved = projectMemberRepository.save(member);
    return new ProjectMemberResponse(saved, user);
  }

  @Transactional
  public void removeMember(Long projectId, Long userId, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    if (userId.equals(currentUserId)) {
      throw new IllegalArgumentException("不能将自己移出项目");
    }
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    if (member.getRole() == ProjectRole.PROJECT_ADMIN
        && projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.PROJECT_ADMIN)
            == 1) {
      throw new IllegalArgumentException("项目必须至少保留一个管理员");
    }
    projectMemberRepository.delete(member);
  }

  @Transactional
  public ProjectMemberResponse updateMemberRole(
      Long projectId, Long userId, UpdateMemberRoleRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    member.setRole(request.getRole());
    ProjectMember saved = projectMemberRepository.save(member);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return new ProjectMemberResponse(saved, user);
  }
}
