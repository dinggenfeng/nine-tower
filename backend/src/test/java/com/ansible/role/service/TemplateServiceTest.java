package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.project.entity.ProjectMember;
import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Template;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
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
class TemplateServiceTest {

  @Mock private TemplateRepository templateRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private TemplateService templateService;

  private Role testRole;
  private Template testTemplate;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testTemplate = new Template();
    ReflectionTestUtils.setField(testTemplate, "id", 1L);
    testTemplate.setRoleId(1L);
    testTemplate.setParentDir(null);
    testTemplate.setName("nginx.conf.j2");
    testTemplate.setTargetPath("/etc/nginx/nginx.conf");
    testTemplate.setContent("server { listen {{ http_port }}; }");
    testTemplate.setCreatedBy(10L);
    ReflectionTestUtils.setField(testTemplate, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testTemplate, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createTemplate_success() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");
    request.setTargetPath("/etc/nginx/nginx.conf");
    request.setContent("server { listen {{ http_port }}; }");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "nginx.conf.j2"))
        .thenReturn(false);
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.createTemplate(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("nginx.conf.j2");
    assertThat(response.getTargetPath()).isEqualTo("/etc/nginx/nginx.conf");
    assertThat(response.getContent()).isEqualTo("server { listen {{ http_port }}; }");
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void createTemplate_duplicateName() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "nginx.conf.j2"))
        .thenReturn(true);

    assertThatThrownBy(() -> templateService.createTemplate(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template with this name already exists in this directory");
  }

  @Test
  void createTemplate_roleNotFound() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.createTemplate(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void createTemplate_withParentDir() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("vhost.conf.j2");
    request.setParentDir("nginx/conf.d");
    request.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    request.setContent("server { server_name {{ domain }}; }");

    Template savedTemplate = new Template();
    ReflectionTestUtils.setField(savedTemplate, "id", 2L);
    savedTemplate.setRoleId(1L);
    savedTemplate.setParentDir("nginx/conf.d");
    savedTemplate.setName("vhost.conf.j2");
    savedTemplate.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    savedTemplate.setContent("server { server_name {{ domain }}; }");
    savedTemplate.setCreatedBy(10L);
    ReflectionTestUtils.setField(savedTemplate, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(savedTemplate, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, "nginx/conf.d", "vhost.conf.j2"))
        .thenReturn(false);
    when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);

    TemplateResponse response = templateService.createTemplate(1L, request, 10L);

    assertThat(response.getParentDir()).isEqualTo("nginx/conf.d");
    assertThat(response.getName()).isEqualTo("vhost.conf.j2");
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void getTemplatesByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L))
        .thenReturn(List.of(testTemplate));

    List<TemplateResponse> templates = templateService.getTemplatesByRole(1L, 10L);

    assertThat(templates).hasSize(1);
    assertThat(templates.get(0).getName()).isEqualTo("nginx.conf.j2");
  }

  @Test
  void getTemplate_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    TemplateResponse response = templateService.getTemplate(1L, 10L);

    assertThat(response.getName()).isEqualTo("nginx.conf.j2");
    assertThat(response.getContent()).isEqualTo("server { listen {{ http_port }}; }");
  }

  @Test
  void getTemplate_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.getTemplate(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void updateTemplate_success() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setContent("server { listen {{ new_port }}; }");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.updateTemplate(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void updateTemplate_notFound() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setContent("new content");

    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.updateTemplate(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void updateTemplate_duplicateName() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setName("other.conf.j2");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "other.conf.j2"))
        .thenReturn(true);

    assertThatThrownBy(() -> templateService.updateTemplate(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template with this name already exists in this directory");
  }

  @Test
  void updateTemplate_nameNotChanged() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.updateTemplate(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void deleteTemplate_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    templateService.deleteTemplate(1L, 10L);

    verify(templateRepository).delete(testTemplate);
  }

  @Test
  void deleteTemplate_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.deleteTemplate(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void getTemplateContent_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    String content = templateService.getTemplateContent(1L, 10L);

    assertThat(content).isEqualTo("server { listen {{ http_port }}; }");
  }

  @Test
  void getTemplateContent_nullReturnsEmpty() {
    testTemplate.setContent(null);
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    String content = templateService.getTemplateContent(1L, 10L);

    assertThat(content).isEmpty();
  }

  @Test
  void getTemplateContent_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.getTemplateContent(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void getTemplateName_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(testTemplate.getRoleId())).thenReturn(Optional.of(testRole));
    when(accessChecker.checkMembership(testRole.getProjectId(), 10L)).thenReturn(new ProjectMember());

    String name = templateService.getTemplateName(1L, 10L);

    assertThat(name).isEqualTo("nginx.conf.j2");
  }

  @Test
  void getTemplateName_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.getTemplateName(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void getTemplateName_withoutMembership_throwsSecurityException() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(testTemplate.getRoleId())).thenReturn(Optional.of(testRole));
    when(accessChecker.checkMembership(testRole.getProjectId(), 2L))
        .thenThrow(new SecurityException("Not a member of this project"));

    assertThatThrownBy(() -> templateService.getTemplateName(1L, 2L))
        .isInstanceOf(SecurityException.class);
  }
}
