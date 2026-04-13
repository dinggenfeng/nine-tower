package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleService roleService;

  private Role testRole;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setDescription("Install and configure nginx");
    testRole.setCreatedBy(10L);
    ReflectionTestUtils.setField(testRole, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testRole, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createRole_success() {
    CreateRoleRequest request = new CreateRoleRequest();
    request.setName("nginx");
    request.setDescription("Install nginx");

    when(roleRepository.save(any(Role.class))).thenReturn(testRole);

    RoleResponse response = roleService.createRole(10L, request, 10L);

    assertThat(response.getName()).isEqualTo("nginx");
    verify(roleRepository).save(any(Role.class));
  }

  @Test
  void getRolesByProject_success() {
    when(roleRepository.findAllByProjectId(10L)).thenReturn(List.of(testRole));

    List<RoleResponse> roles = roleService.getRolesByProject(10L, 10L);

    assertThat(roles).hasSize(1);
    assertThat(roles.get(0).getName()).isEqualTo("nginx");
  }

  @Test
  void getRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    RoleResponse response = roleService.getRole(1L, 10L);

    assertThat(response.getName()).isEqualTo("nginx");
  }

  @Test
  void updateRole_success() {
    UpdateRoleRequest request = new UpdateRoleRequest();
    request.setName("apache");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleRepository.save(any(Role.class))).thenReturn(testRole);

    roleService.updateRole(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
  }

  @Test
  void deleteRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleService.deleteRole(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(roleRepository).delete(testRole);
  }
}
