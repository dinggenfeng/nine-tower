package com.ansible.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private ProjectMemberService projectMemberService;

  private User testUser;
  private ProjectMember testMember;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(20L);
    testUser.setUsername("bob");
    testUser.setEmail("bob@example.com");
    testUser.setPassword("encoded");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());

    testMember = new ProjectMember();
    testMember.setId(1L);
    testMember.setProjectId(1L);
    testMember.setUserId(20L);
    testMember.setRole(ProjectRole.PROJECT_MEMBER);
    testMember.setJoinedAt(LocalDateTime.now());
  }

  @Test
  void listMembers_success() {
    when(projectMemberRepository.findAllByProjectId(1L)).thenReturn(List.of(testMember));
    when(userRepository.findAllById(List.of(20L))).thenReturn(List.of(testUser));

    List<ProjectMemberResponse> members = projectMemberService.listMembers(1L, 10L);

    assertThat(members).hasSize(1);
    assertThat(members.get(0).getUsername()).isEqualTo("bob");
    assertThat(members.get(0).getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
    verify(accessChecker).checkMembership(1L, 10L);
  }

  @Test
  void addMember_success() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(20L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));
    when(projectMemberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(false);
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

    ProjectMemberResponse response = projectMemberService.addMember(1L, request, 10L);

    assertThat(response.getUsername()).isEqualTo("bob");
    verify(accessChecker).checkAdmin(1L, 10L);
  }

  @Test
  void addMember_fails_when_already_member() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(20L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));
    when(projectMemberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(true);

    assertThatThrownBy(() -> projectMemberService.addMember(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User is already a member");
  }

  @Test
  void addMember_fails_when_user_not_found() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(99L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectMemberService.addMember(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void removeMember_success() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(testMember));

    projectMemberService.removeMember(1L, 20L, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    verify(projectMemberRepository).delete(testMember);
  }

  @Test
  void removeMember_fails_when_not_member() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Member not found");
  }

  @Test
  void removeMember_fails_when_removing_self() {
    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("不能将自己移出项目");
  }

  @Test
  void removeMember_fails_when_removing_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(1L);

    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 20L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("项目必须至少保留一个管理员");
  }

  @Test
  void updateMemberRole_success() {
    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_ADMIN);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(testMember));
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);
    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));

    ProjectMemberResponse response =
        projectMemberService.updateMemberRole(1L, 20L, request, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    assertThat(testMember.getRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void updateMemberRole_fails_when_changing_own_role() {
    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    assertThatThrownBy(() -> projectMemberService.updateMemberRole(1L, 10L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("不能修改自己的角色");
  }

  @Test
  void updateMemberRole_fails_when_downgrading_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(1L);

    assertThatThrownBy(() -> projectMemberService.updateMemberRole(1L, 20L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("项目必须至少保留一个管理员");
  }

  @Test
  void updateMemberRole_success_when_downgrading_non_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setId(1L);
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(2L);
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(adminMember);
    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));

    ProjectMemberResponse response =
        projectMemberService.updateMemberRole(1L, 20L, request, 10L);

    assertThat(adminMember.getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
    verify(projectMemberRepository).save(adminMember);
  }
}
