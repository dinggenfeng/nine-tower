package com.ansible.variable.service;

import com.ansible.role.entity.*;
import com.ansible.role.repository.*;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariableDetectionServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private HandlerRepository handlerRepository;
  @Mock private TemplateRepository templateRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private VariableDetectionService service;

  private Role role1, role2;
  private Task task1;
  private Handler handler1;
  private Template template1;

  @BeforeEach
  void setUp() {
    role1 = new Role(); role1.setProjectId(1L); role1.setName("web");
    ReflectionTestUtils.setField(role1, "id", 1L);
    role2 = new Role(); role2.setProjectId(1L); role2.setName("api");
    ReflectionTestUtils.setField(role2, "id", 2L);

    task1 = new Task(); task1.setRoleId(1L); task1.setName("启动 {{ app_port }}");
    task1.setModule("shell"); task1.setTaskOrder(1);
    task1.setArgs("{\"cmd\":\"start --port {{ app_port }}\"}");
    task1.setWhenCondition("ansible_os_family == '{{ target_os }}'");
    ReflectionTestUtils.setField(task1, "id", 10L);

    handler1 = new Handler(); handler1.setRoleId(2L); handler1.setName("重启服务");
    handler1.setModule("systemd");
    handler1.setArgs("{\"name\":\"{{ app_port }}\"}");
    ReflectionTestUtils.setField(handler1, "id", 20L);

    template1 = new Template(); template1.setRoleId(2L); template1.setName("nginx.conf");
    template1.setContent("server { listen {{ app_port }}; }");
    template1.setIsDirectory(false);
    ReflectionTestUtils.setField(template1, "id", 30L);
  }

  @Test
  void detectVariables_extractsVarsAndInfersScope() {
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of(role1, role2));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task1));
    when(handlerRepository.findAllByRoleId(1L)).thenReturn(List.of());
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L)).thenReturn(List.of());
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(2L)).thenReturn(List.of());
    when(handlerRepository.findAllByRoleId(2L)).thenReturn(List.of(handler1));
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(2L)).thenReturn(List.of(template1));

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result).hasSize(2);

    // app_port appears in both roles -> PROJECT scope
    DetectedVariableResponse appPort = result.stream()
        .filter(r -> r.key().equals("app_port")).findFirst().orElseThrow();
    assertThat(appPort.suggestedScope()).isEqualTo("PROJECT");
    assertThat(appPort.occurrences()).hasSize(4);

    // target_os appears in one role -> ROLE scope
    DetectedVariableResponse targetOs = result.stream()
        .filter(r -> r.key().equals("target_os")).findFirst().orElseThrow();
    assertThat(targetOs.suggestedScope()).isEqualTo("ROLE");
    assertThat(targetOs.occurrences()).hasSize(1);
  }

  @Test
  void detectVariables_excludesBuiltinVars() {
    task1.setLoop("{{ item.name }}");
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of(role1));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task1));
    when(handlerRepository.findAllByRoleId(1L)).thenReturn(List.of());
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L)).thenReturn(List.of());

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result.stream().anyMatch(r -> r.key().startsWith("item"))).isFalse();
  }

  @Test
  void detectVariables_emptyWhenNoRoles() {
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of());

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result).isEmpty();
  }
}
