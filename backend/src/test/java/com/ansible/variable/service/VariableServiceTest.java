package com.ansible.variable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
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
class VariableServiceTest {

  @Mock private VariableRepository variableRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private VariableService variableService;

  private Variable testVariable;

  @BeforeEach
  void setUp() {
    testVariable = new Variable();
    ReflectionTestUtils.setField(testVariable, "id", 1L);
    testVariable.setScope(VariableScope.PROJECT);
    testVariable.setScopeId(10L);
    testVariable.setKey("APP_PORT");
    testVariable.setValue("8080");
    testVariable.setCreatedBy(100L);
    ReflectionTestUtils.setField(testVariable, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testVariable, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createVariable_projectScope_success() {
    when(variableRepository.existsByScopeAndScopeIdAndKey(VariableScope.PROJECT, 10L, "APP_PORT"))
        .thenReturn(false);
    when(variableRepository.save(any(Variable.class))).thenReturn(testVariable);

    VariableResponse response =
        variableService.createVariable(
            10L, new CreateVariableRequest(VariableScope.PROJECT, 10L, "APP_PORT", "8080"), 100L);

    assertThat(response.key()).isEqualTo("APP_PORT");
    assertThat(response.scope()).isEqualTo(VariableScope.PROJECT);
    verify(accessChecker).checkMembership(10L, 100L);
  }

  @Test
  void createVariable_duplicateKey_throws() {
    when(variableRepository.existsByScopeAndScopeIdAndKey(VariableScope.PROJECT, 10L, "APP_PORT"))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                variableService.createVariable(
                    10L,
                    new CreateVariableRequest(VariableScope.PROJECT, 10L, "APP_PORT", "8080"),
                    100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void listVariables_byScopeAndScopeId() {
    Variable var2 = new Variable();
    ReflectionTestUtils.setField(var2, "id", 2L);
    var2.setScope(VariableScope.PROJECT);
    var2.setScopeId(10L);
    var2.setKey("APP_HOST");
    var2.setValue("localhost");

    when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 10L))
        .thenReturn(List.of(testVariable, var2));

    List<VariableResponse> list =
        variableService.listVariables(10L, VariableScope.PROJECT, 10L, 100L);

    assertThat(list).hasSize(2);
    verify(accessChecker).checkMembership(10L, 100L);
  }

  @Test
  void listVariables_byScopeOnly() {
    when(variableRepository.findByScopeOrderByIdAsc(VariableScope.PROJECT))
        .thenReturn(List.of(testVariable));

    List<VariableResponse> list =
        variableService.listVariables(10L, VariableScope.PROJECT, null, 100L);

    assertThat(list).hasSize(1);
    verify(accessChecker).checkMembership(10L, 100L);
  }

  @Test
  void getVariable_success() {
    when(variableRepository.findById(1L)).thenReturn(Optional.of(testVariable));

    VariableResponse response = variableService.getVariable(1L, 100L);

    assertThat(response.key()).isEqualTo("APP_PORT");
    verify(accessChecker).checkMembership(10L, 100L);
  }

  @Test
  void getVariable_notFound_throws() {
    when(variableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> variableService.getVariable(99L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable not found");
  }

  @Test
  void updateVariable_success() {
    when(variableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(
            VariableScope.PROJECT, 10L, "PORT", 1L))
        .thenReturn(false);
    when(variableRepository.save(any(Variable.class))).thenReturn(testVariable);

    VariableResponse response =
        variableService.updateVariable(1L, new UpdateVariableRequest("PORT", "9090"), 100L);

    assertThat(response.key()).isEqualTo("PORT");
    verify(accessChecker).checkOwnerOrAdmin(10L, 100L, 100L);
  }

  @Test
  void updateVariable_duplicateKey_throws() {
    when(variableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(
            VariableScope.PROJECT, 10L, "PORT", 1L))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                variableService.updateVariable(
                    1L, new UpdateVariableRequest("PORT", "9090"), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void updateVariable_notFound_throws() {
    when(variableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                variableService.updateVariable(
                    99L, new UpdateVariableRequest("PORT", "9090"), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable not found");
  }

  @Test
  void deleteVariable_success() {
    when(variableRepository.findById(1L)).thenReturn(Optional.of(testVariable));

    variableService.deleteVariable(1L, 100L);

    verify(variableRepository).delete(testVariable);
    verify(accessChecker).checkOwnerOrAdmin(10L, 100L, 100L);
  }

  @Test
  void deleteVariable_notFound_throws() {
    when(variableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> variableService.deleteVariable(99L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable not found");
  }
}
