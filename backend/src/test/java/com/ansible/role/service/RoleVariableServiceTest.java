package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
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
class RoleVariableServiceTest {

  @Mock private RoleVariableRepository roleVariableRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleVariableService roleVariableService;

  private Role testRole;
  private RoleVariable testVariable;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testVariable = new RoleVariable();
    ReflectionTestUtils.setField(testVariable, "id", 1L);
    testVariable.setRoleId(1L);
    testVariable.setKey("http_port");
    testVariable.setValue("8080");
    testVariable.setCreatedBy(10L);
    ReflectionTestUtils.setField(testVariable, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testVariable, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createVariable_success() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");
    request.setValue("8080");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(false);
    when(roleVariableRepository.save(any(RoleVariable.class))).thenReturn(testVariable);

    RoleVariableResponse response = roleVariableService.createVariable(1L, request, 10L);

    assertThat(response.getKey()).isEqualTo("http_port");
    assertThat(response.getValue()).isEqualTo("8080");
    verify(roleVariableRepository).save(any(RoleVariable.class));
  }

  @Test
  void createVariable_duplicateKey() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");
    request.setValue("8080");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(true);

    assertThatThrownBy(() -> roleVariableService.createVariable(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable key already exists in this role");
  }

  @Test
  void createVariable_roleNotFound() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.createVariable(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getVariablesByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.findAllByRoleIdOrderByKeyAsc(1L))
        .thenReturn(List.of(testVariable));

    List<RoleVariableResponse> variables = roleVariableService.getVariablesByRole(1L, 10L);

    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getKey()).isEqualTo("http_port");
  }

  @Test
  void updateVariable_success() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setValue("9090");

    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.save(any(RoleVariable.class))).thenReturn(testVariable);

    RoleVariableResponse response = roleVariableService.updateVariable(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleVariableRepository).save(any(RoleVariable.class));
  }

  @Test
  void updateVariable_notFound() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setValue("9090");

    when(roleVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.updateVariable(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role variable not found");
  }

  @Test
  void deleteVariable_success() {
    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleVariableService.deleteVariable(1L, 10L);

    verify(roleVariableRepository).delete(testVariable);
  }

  @Test
  void deleteVariable_notFound() {
    when(roleVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.deleteVariable(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role variable not found");
  }
}
