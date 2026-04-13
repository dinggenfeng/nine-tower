package com.ansible.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAccessCheckerTest {

  @Mock private ProjectMemberRepository projectMemberRepository;
  @InjectMocks private ProjectAccessChecker accessChecker;

  private ProjectMember adminMember;

  @BeforeEach
  void setUp() {
    adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(10L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void checkOwnerOrAdmin_passes_when_user_is_owner() {
    // no exception - owner bypasses membership check
    accessChecker.checkOwnerOrAdmin(1L, 10L, 10L);
  }

  @Test
  void checkOwnerOrAdmin_passes_when_user_is_admin() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(java.util.Optional.of(adminMember));
    // no exception
    accessChecker.checkOwnerOrAdmin(1L, 10L, 20L);
  }

  @Test
  void checkOwnerOrAdmin_throws_when_neither_owner_nor_admin() {
    ProjectMember regularMember = new ProjectMember();
    regularMember.setProjectId(1L);
    regularMember.setUserId(20L);
    regularMember.setRole(ProjectRole.PROJECT_MEMBER);
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(java.util.Optional.of(regularMember));

    assertThatThrownBy(() -> accessChecker.checkOwnerOrAdmin(1L, 10L, 20L))
        .isInstanceOf(SecurityException.class);
  }
}
