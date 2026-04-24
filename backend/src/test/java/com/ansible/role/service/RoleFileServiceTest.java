package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleFile;
import com.ansible.role.repository.RoleFileRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleFileServiceTest {

  @Mock private RoleFileRepository roleFileRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private RoleFileService roleFileService;

  private RoleFile createFile(Long id, Long roleId, String parentDir, String name, boolean isDir) {
    RoleFile f = new RoleFile();
    ReflectionTestUtils.setField(f, "id", id);
    f.setRoleId(roleId);
    f.setParentDir(parentDir);
    f.setName(name);
    f.setIsDirectory(isDir);
    if (!isDir) {
      f.setContent("content".getBytes(StandardCharsets.UTF_8));
    }
    return f;
  }

  private void stubRole(Long roleId) {
    Role role = new Role();
    role.setProjectId(1L);
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
  }

  @Test
  void createFile_success() {
    stubRole(1L);
    when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "config.yml"))
        .thenReturn(false);
    when(roleFileRepository.save(any(RoleFile.class)))
        .thenAnswer(
            inv -> {
              RoleFile f = inv.getArgument(0);
              ReflectionTestUtils.setField(f, "id", 10L);
              return f;
            });

    CreateFileRequest request = new CreateFileRequest();
    request.setParentDir("");
    request.setName("config.yml");
    request.setIsDirectory(false);
    FileResponse response =
        roleFileService.createFile(
            1L, request, "content".getBytes(StandardCharsets.UTF_8), 100L);

    assertThat(response.getName()).isEqualTo("config.yml");
    assertThat(response.getIsDirectory()).isFalse();
  }

  @Test
  void createFile_duplicateName_throws() {
    stubRole(1L);
    when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "config.yml"))
        .thenReturn(true);

    CreateFileRequest request = new CreateFileRequest();
    request.setParentDir("");
    request.setName("config.yml");
    request.setIsDirectory(false);
    assertThatThrownBy(
            () ->
                roleFileService.createFile(
                    1L, request, "content".getBytes(StandardCharsets.UTF_8), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void createDirectory_success() {
    stubRole(1L);
    when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "conf.d"))
        .thenReturn(false);
    when(roleFileRepository.save(any(RoleFile.class)))
        .thenAnswer(
            inv -> {
              RoleFile f = inv.getArgument(0);
              ReflectionTestUtils.setField(f, "id", 10L);
              return f;
            });

    CreateFileRequest request = new CreateFileRequest();
    request.setParentDir("");
    request.setName("conf.d");
    request.setIsDirectory(true);
    FileResponse response = roleFileService.createFile(1L, request, null, 100L);

    assertThat(response.getName()).isEqualTo("conf.d");
    assertThat(response.getIsDirectory()).isTrue();
  }

  @Test
  void listFiles_treeStructure() {
    stubRole(1L);
    when(roleFileRepository.findByRoleIdOrderByParentDirAscNameAsc(1L))
        .thenReturn(
            List.of(
                createFile(1L, 1L, "", "conf.d", true),
                createFile(2L, 1L, "conf.d", "app.conf", false),
                createFile(3L, 1L, "", "readme.txt", false)));

    List<FileResponse> tree = roleFileService.listFilesAsTree(1L, 100L);

    assertThat(tree).hasSize(2);
    assertThat(tree.get(0).getName()).isEqualTo("conf.d");
    assertThat(tree.get(0).getChildren()).hasSize(1);
    assertThat(tree.get(1).getName()).isEqualTo("readme.txt");
  }

  @Test
  void getFile_success() {
    stubRole(1L);
    RoleFile file = createFile(1L, 1L, "", "config.yml", false);
    file.setContent("hello world".getBytes(StandardCharsets.UTF_8));
    when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));

    FileResponse response = roleFileService.getFile(1L, 100L);

    assertThat(response.getName()).isEqualTo("config.yml");
    assertThat(response.getTextContent()).isEqualTo("hello world");
  }

  @Test
  void updateFileName_success() {
    stubRole(1L);
    RoleFile file = createFile(1L, 1L, "", "old.yml", false);
    when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));
    when(roleFileRepository.existsByRoleIdAndParentDirAndNameAndIdNot(1L, "", "new.yml", 1L))
        .thenReturn(false);
    when(roleFileRepository.save(any(RoleFile.class))).thenReturn(file);

    UpdateFileRequest request = new UpdateFileRequest();
    request.setName("new.yml");
    request.setTextContent(null);
    FileResponse response = roleFileService.updateFile(1L, request, 100L);

    assertThat(response.getName()).isEqualTo("new.yml");
  }

  @Test
  void updateFileContent_success() {
    stubRole(1L);
    RoleFile file = createFile(1L, 1L, "", "config.yml", false);
    when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));
    when(roleFileRepository.save(any(RoleFile.class))).thenReturn(file);

    UpdateFileRequest request = new UpdateFileRequest();
    request.setName("config.yml");
    request.setTextContent("new content");
    FileResponse response = roleFileService.updateFile(1L, request, 100L);

    assertThat(response.getTextContent()).isEqualTo("new content");
  }

  @Test
  void deleteFile_success() {
    stubRole(1L);
    RoleFile file = createFile(1L, 1L, "", "config.yml", false);
    when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));

    roleFileService.deleteFile(1L, 100L);

    verify(roleFileRepository).delete(file);
  }

  @Test
  void deleteDirectory_cascadesChildren() {
    stubRole(1L);
    RoleFile dir = createFile(1L, 1L, "", "conf.d", true);
    RoleFile child = createFile(2L, 1L, "conf.d", "app.conf", false);
    when(roleFileRepository.findById(1L)).thenReturn(Optional.of(dir));
    when(roleFileRepository.findByRoleIdAndParentDirStartingWith(1L, "conf.d/"))
        .thenReturn(List.of(child));

    roleFileService.deleteFile(1L, 100L);

    verify(roleFileRepository).deleteAll(List.of(child));
    verify(roleFileRepository).delete(dir);
  }

  @Test
  void getFileContent_withoutMembership_throwsSecurityException() {
    RoleFile file = createFile(10L, 1L, "", "test.yml", false);
    when(roleFileRepository.findById(10L)).thenReturn(Optional.of(file));

    Role role = new Role();
    role.setProjectId(100L);
    when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
    when(accessChecker.checkMembership(100L, 2L))
        .thenThrow(new SecurityException("Not a member of this project"));

    assertThatThrownBy(() -> roleFileService.getFileContent(10L, 2L))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void getFileName_withoutMembership_throwsSecurityException() {
    RoleFile file = createFile(10L, 1L, "", "test.yml", false);
    when(roleFileRepository.findById(10L)).thenReturn(Optional.of(file));

    Role role = new Role();
    role.setProjectId(100L);
    when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
    when(accessChecker.checkMembership(100L, 2L))
        .thenThrow(new SecurityException("Not a member of this project"));

    assertThatThrownBy(() -> roleFileService.getFileName(10L, 2L))
        .isInstanceOf(SecurityException.class);
  }
}
