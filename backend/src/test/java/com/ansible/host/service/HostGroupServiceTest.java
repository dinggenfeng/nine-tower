package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.project.service.ProjectCleanupService;
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
class HostGroupServiceTest {

  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @Mock private ProjectCleanupService cleanupService;
  @Mock private HostRepository hostRepository;
  @InjectMocks private HostGroupService hostGroupService;

  private HostGroup testHostGroup;

  @BeforeEach
  void setUp() {
    testHostGroup = new HostGroup();
    ReflectionTestUtils.setField(testHostGroup, "id", 1L);
    testHostGroup.setProjectId(10L);
    testHostGroup.setName("Web Servers");
    testHostGroup.setDescription("All web servers");
    testHostGroup.setCreatedBy(10L);
    ReflectionTestUtils.setField(testHostGroup, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testHostGroup, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createHostGroup_success() {
    CreateHostGroupRequest request = new CreateHostGroupRequest();
    request.setName("Web Servers");
    request.setDescription("All web servers");

    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers")).thenReturn(false);
    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(testHostGroup);

    HostGroupResponse response = hostGroupService.createHostGroup(10L, request, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers");
    verify(hostGroupRepository).save(any(HostGroup.class));
  }

  @Test
  void createHostGroup_fails_when_name_duplicate() {
    CreateHostGroupRequest request = new CreateHostGroupRequest();
    request.setName("Web Servers");

    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers")).thenReturn(true);

    assertThatThrownBy(() -> hostGroupService.createHostGroup(10L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void getHostGroupsByProject_success() {
    when(hostGroupRepository.findAllByProjectId(10L)).thenReturn(List.of(testHostGroup));

    List<HostGroupResponse> groups = hostGroupService.getHostGroupsByProject(10L, 10L);

    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getName()).isEqualTo("Web Servers");
  }

  @Test
  void getHostGroup_success() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    HostGroupResponse response = hostGroupService.getHostGroup(1L, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers");
  }

  @Test
  void getHostGroup_notFound_throws() {
    when(hostGroupRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hostGroupService.getHostGroup(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void updateHostGroup_success() {
    UpdateHostGroupRequest request = new UpdateHostGroupRequest();
    request.setName("Updated Name");

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(testHostGroup);

    hostGroupService.updateHostGroup(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostGroupRepository).save(testHostGroup);
  }

  @Test
  void deleteHostGroup_success() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    hostGroupService.deleteHostGroup(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(cleanupService).cleanupHostGroupResources(1L);
    verify(hostGroupRepository).delete(testHostGroup);
  }

  @Test
  void copyHostGroup_copiesHostGroupAndHosts() {
    Host sourceHost = new Host();
    ReflectionTestUtils.setField(sourceHost, "id", 10L);
    sourceHost.setHostGroupId(1L);
    sourceHost.setName("web-01");
    sourceHost.setIp("192.168.1.10");
    sourceHost.setPort(22);
    sourceHost.setAnsibleUser("ansible");
    sourceHost.setAnsibleSshPass("encrypted-pass");
    sourceHost.setAnsibleBecome(false);
    sourceHost.setCreatedBy(10L);

    HostGroup copiedHostGroup = new HostGroup();
    ReflectionTestUtils.setField(copiedHostGroup, "id", 2L);
    copiedHostGroup.setProjectId(10L);
    copiedHostGroup.setName("Web Servers (副本)");
    copiedHostGroup.setDescription("All web servers");
    copiedHostGroup.setCreatedBy(10L);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)"))
        .thenReturn(false);
    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(copiedHostGroup);
    when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of(sourceHost));
    when(hostRepository.save(any(Host.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HostGroupResponse response = hostGroupService.copyHostGroup(1L, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers (副本)");
    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostGroupRepository).save(any(HostGroup.class));
    verify(hostRepository).findAllByHostGroupId(1L);
    verify(hostRepository).save(any(Host.class));
  }

  @Test
  void copyHostGroup_generatesUniqueName_whenDuplicate() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)"))
        .thenReturn(true);
    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)2"))
        .thenReturn(true);
    when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)3"))
        .thenReturn(false);

    HostGroup copiedHostGroup = new HostGroup();
    ReflectionTestUtils.setField(copiedHostGroup, "id", 2L);
    copiedHostGroup.setName("Web Servers (副本)3");
    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(copiedHostGroup);
    when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of());

    HostGroupResponse response = hostGroupService.copyHostGroup(1L, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers (副本)3");
  }

  @Test
  void copyHostGroup_hostGroupNotFound_throws() {
    when(hostGroupRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hostGroupService.copyHostGroup(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}
