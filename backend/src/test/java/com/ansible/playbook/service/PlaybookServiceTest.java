package com.ansible.playbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookResponse;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.entity.Playbook;
import com.ansible.playbook.entity.PlaybookHostGroup;
import com.ansible.playbook.entity.PlaybookRole;
import com.ansible.playbook.entity.PlaybookTag;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.playbook.yaml.PlaybookYamlGenerator;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaybookServiceTest {

  @Mock private PlaybookRepository playbookRepository;
  @Mock private PlaybookRoleRepository playbookRoleRepository;
  @Mock private PlaybookHostGroupRepository playbookHostGroupRepository;
  @Mock private PlaybookTagRepository playbookTagRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private TagRepository tagRepository;
  @Mock private PlaybookYamlGenerator yamlGenerator;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private PlaybookService playbookService;

  @Test
  void createPlaybook_success() {
    Playbook saved = new Playbook();
    ReflectionTestUtils.setField(saved, "id", 1L);
    saved.setProjectId(1L);
    saved.setName("deploy.yml");
    saved.setCreatedBy(100L);
    when(playbookRepository.save(any(Playbook.class))).thenReturn(saved);

    PlaybookResponse response =
        playbookService.createPlaybook(
            1L, new CreatePlaybookRequest("deploy.yml", "Deploy app", null), 100L);

    assertThat(response.name()).isEqualTo("deploy.yml");
  }

  @Test
  void listPlaybooks_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    p.setName("deploy.yml");
    p.setCreatedBy(100L);
    when(playbookRepository.findByProjectIdOrderByIdAsc(1L)).thenReturn(List.of(p));
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of());
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());

    List<PlaybookResponse> list = playbookService.listPlaybooks(1L, 100L);
    assertThat(list).hasSize(1);
  }

  @Test
  void addRole_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(false);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    PlaybookRole savedRole = new PlaybookRole();
    ReflectionTestUtils.setField(savedRole, "id", 100L);
    when(playbookRoleRepository.save(any(PlaybookRole.class))).thenReturn(savedRole);

    playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L);

    verify(playbookRoleRepository).save(any(PlaybookRole.class));
  }

  @Test
  void addRole_duplicate_throws() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(true);

    assertThatThrownBy(() -> playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already added");
  }

  @Test
  void removeRole_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    playbookService.removeRole(1L, 10L, 100L);
    verify(playbookRoleRepository).deleteByPlaybookIdAndRoleId(1L, 10L);
  }

  @Test
  void addHostGroup_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
    when(playbookHostGroupRepository.existsByPlaybookIdAndHostGroupId(1L, 5L)).thenReturn(false);
    PlaybookHostGroup savedHg = new PlaybookHostGroup();
    ReflectionTestUtils.setField(savedHg, "id", 100L);
    when(playbookHostGroupRepository.save(any(PlaybookHostGroup.class))).thenReturn(savedHg);

    playbookService.addHostGroup(1L, 5L, 100L);
    verify(playbookHostGroupRepository).save(any(PlaybookHostGroup.class));
  }

  @Test
  void generateYaml_success() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    PlaybookRole pr = new PlaybookRole();
    pr.setRoleId(10L);
    pr.setOrderIndex(0);
    when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(pr));

    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 10L);
    role.setName("nginx");
    when(roleRepository.findById(10L)).thenReturn(Optional.of(role));

    PlaybookHostGroup phg = new PlaybookHostGroup();
    phg.setHostGroupId(5L);
    when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of(phg));

    HostGroup hg = new HostGroup();
    ReflectionTestUtils.setField(hg, "id", 5L);
    hg.setName("web_servers");
    when(hostGroupRepository.findById(5L)).thenReturn(Optional.of(hg));

    PlaybookTag pt = new PlaybookTag();
    pt.setTagId(3L);
    when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of(pt));

    Tag tag = new Tag();
    ReflectionTestUtils.setField(tag, "id", 3L);
    tag.setName("deploy");
    when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));

    when(yamlGenerator.generate(List.of("nginx"), List.of("web_servers"), List.of("deploy")))
        .thenReturn("- hosts: web_servers\n  roles:\n    - nginx\n  tags: [deploy]\n");

    String yaml = playbookService.generateYaml(1L, 100L);

    assertThat(yaml).contains("nginx", "web_servers", "deploy");
  }

  @Test
  void deletePlaybook_cascades() {
    Playbook p = new Playbook();
    ReflectionTestUtils.setField(p, "id", 1L);
    p.setProjectId(1L);
    when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

    playbookService.deletePlaybook(1L, 100L);

    verify(playbookRoleRepository).deleteByPlaybookId(1L);
    verify(playbookHostGroupRepository).deleteByPlaybookId(1L);
    verify(playbookTagRepository).deleteByPlaybookId(1L);
    verify(playbookRepository).delete(p);
  }
}
