package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.common.EncryptionService;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
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
class HostServiceTest {

  @Mock private HostRepository hostRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @Mock private EncryptionService encryptionService;
  @InjectMocks private HostService hostService;

  private Host testHost;
  private HostGroup testHostGroup;

  @BeforeEach
  void setUp() {
    testHostGroup = new HostGroup();
    ReflectionTestUtils.setField(testHostGroup, "id", 1L);
    testHostGroup.setProjectId(10L);

    testHost = new Host();
    ReflectionTestUtils.setField(testHost, "id", 1L);
    testHost.setHostGroupId(1L);
    testHost.setName("web-01");
    testHost.setIp("192.168.1.10");
    testHost.setPort(22);
    testHost.setAnsibleUser("ansible");
    testHost.setAnsibleSshPass("encrypted");
    testHost.setAnsibleSshPrivateKeyFile("encrypted");
    testHost.setAnsibleBecome(false);
    testHost.setCreatedBy(10L);
    ReflectionTestUtils.setField(testHost, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testHost, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createHost_success() {
    CreateHostRequest request = new CreateHostRequest();
    request.setName("web-01");
    request.setIp("192.168.1.10");
    request.setPort(22);
    request.setAnsibleUser("ansible");
    request.setAnsibleSshPass("plaintext");
    request.setAnsibleBecome(false);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(encryptionService.encrypt("plaintext")).thenReturn("encrypted");
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    HostResponse response = hostService.createHost(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("web-01");
    verify(encryptionService).encrypt("plaintext");
  }

  @Test
  void getHostsByHostGroup_success() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of(testHost));

    List<HostResponse> hosts = hostService.getHostsByHostGroup(1L, 10L);

    assertThat(hosts).hasSize(1);
    assertThat(hosts.get(0).getName()).isEqualTo("web-01");
  }

  @Test
  void getHost_success() {
    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    HostResponse response = hostService.getHost(1L, 10L);

    assertThat(response.getName()).isEqualTo("web-01");
    assertThat(response.getAnsibleSshPass()).isEqualTo("****");
  }

  @Test
  void updateHost_success() {
    UpdateHostRequest request = new UpdateHostRequest();
    request.setName("web-02");

    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    hostService.updateHost(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    assertThat(testHost.getName()).isEqualTo("web-02");
  }

  @Test
  void deleteHost_success() {
    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    hostService.deleteHost(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostRepository).delete(testHost);
  }

  @Test
  void createHost_withCopyFromHostId_retainsEncryptedFields() {
    Host sourceHost = new Host();
    sourceHost.setAnsibleSshPass("encrypted-source-pass");
    sourceHost.setAnsibleSshPrivateKeyFile("encrypted-source-key");

    CreateHostRequest request = new CreateHostRequest();
    request.setName("web-02");
    request.setIp("192.168.1.11");
    request.setPort(22);
    request.setAnsibleUser("ansible");
    request.setCopyFromHostId(5L);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.findById(5L)).thenReturn(Optional.of(sourceHost));
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    hostService.createHost(1L, request, 10L);

    verify(hostRepository).findById(5L);
    verify(encryptionService, never()).encrypt(any());
  }

  @Test
  void createHost_withCopyFromHostId_userProvidedPasswordOverrides() {
    Host sourceHost = new Host();
    sourceHost.setAnsibleSshPass("encrypted-source-pass");

    CreateHostRequest request = new CreateHostRequest();
    request.setName("web-02");
    request.setIp("192.168.1.11");
    request.setPort(22);
    request.setAnsibleSshPass("new-password");
    request.setCopyFromHostId(5L);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.findById(5L)).thenReturn(Optional.of(sourceHost));
    when(encryptionService.encrypt("new-password")).thenReturn("encrypted-new");
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    hostService.createHost(1L, request, 10L);

    verify(encryptionService).encrypt("new-password");
    verify(hostRepository).findById(5L);
  }

  @Test
  void createHost_withCopyFromHostId_sourceNotFound_doesNotFail() {
    CreateHostRequest request = new CreateHostRequest();
    request.setName("web-02");
    request.setIp("192.168.1.11");
    request.setPort(22);
    request.setCopyFromHostId(99L);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.findById(99L)).thenReturn(Optional.empty());
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    HostResponse response = hostService.createHost(1L, request, 10L);

    assertThat(response).isNotNull();
  }
}