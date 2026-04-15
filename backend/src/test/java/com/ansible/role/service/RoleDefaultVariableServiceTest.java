package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
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
class RoleDefaultVariableServiceTest {

  @Mock private RoleDefaultVariableRepository roleDefaultVariableRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleDefaultVariableService roleDefaultVariableService;

  private Role testRole;
  private RoleDefaultVariable testVariable;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testVariable = new RoleDefaultVariable();
    ReflectionTestUtils.setField(testVariable, "id", 1L);
    testVariable.setRoleId(1L);
    testVariable.setKey("http_port");
    testVariable.setValue("80");
    testVariable.setCreatedBy(10L);
    ReflectionTestUtils.setField(testVariable, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testVariable, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createDefault_success() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");
    request.setValue("80");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(false);
    when(roleDefaultVariableRepository.save(any(RoleDefaultVariable.class)))
        .thenReturn(testVariable);

    RoleDefaultVariableResponse response =
        roleDefaultVariableService.createDefault(1L, request, 10L);

    assertThat(response.getKey()).isEqualTo("http_port");
    assertThat(response.getValue()).isEqualTo("80");
    verify(roleDefaultVariableRepository).save(any(RoleDefaultVariable.class));
  }

  @Test
  void createDefault_duplicateKey() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");
    request.setValue("80");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(true);

    assertThatThrownBy(() -> roleDefaultVariableService.createDefault(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default variable key already exists in this role");
  }

  @Test
  void createDefault_roleNotFound() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.createDefault(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getDefaultsByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(1L))
        .thenReturn(List.of(testVariable));

    List<RoleDefaultVariableResponse> defaults =
        roleDefaultVariableService.getDefaultsByRole(1L, 10L);

    assertThat(defaults).hasSize(1);
    assertThat(defaults.get(0).getKey()).isEqualTo("http_port");
  }

  @Test
  void updateDefault_success() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setValue("8080");

    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.save(any(RoleDefaultVariable.class)))
        .thenReturn(testVariable);

    RoleDefaultVariableResponse response =
        roleDefaultVariableService.updateDefault(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleDefaultVariableRepository).save(any(RoleDefaultVariable.class));
  }

  @Test
  void updateDefault_notFound() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setValue("8080");

    when(roleDefaultVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.updateDefault(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role default variable not found");
  }

  @Test
  void deleteDefault_success() {
    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleDefaultVariableService.deleteDefault(1L, 10L);

    verify(roleDefaultVariableRepository).delete(testVariable);
  }

  @Test
  void deleteDefault_notFound() {
    when(roleDefaultVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.deleteDefault(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role default variable not found");
  }
}
