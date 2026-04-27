package com.ansible.role.service;

import com.ansible.security.ProjectAccessChecker;
import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleFile;
import com.ansible.role.repository.RoleFileRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleFileService {

  private static final long MAX_FILE_SIZE = 500L * 1024 * 1024; // 500MB

  private final RoleFileRepository roleFileRepository;
  private final com.ansible.role.repository.RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public FileResponse createFile(
      Long roleId, CreateFileRequest request, byte[] content, Long userId) {
    Long projectId = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkMembership(projectId, userId);
    boolean isDir = Boolean.TRUE.equals(request.getIsDirectory());
    String parentDir = request.getParentDir() != null ? request.getParentDir() : "";
    if (roleFileRepository.existsByRoleIdAndParentDirAndName(
        roleId, parentDir, request.getName())) {
      throw new IllegalArgumentException(
          "File or directory '" + request.getName() + "' already exists");
    }
    if (!isDir && content != null && content.length > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds 500MB limit");
    }
    RoleFile file = new RoleFile();
    file.setRoleId(roleId);
    file.setParentDir(parentDir);
    file.setName(request.getName());
    file.setTargetPath(request.getTargetPath());
    file.setIsDirectory(isDir);
    file.setContent(isDir ? null : content != null ? content : new byte[0]);
    file.setCreatedBy(userId);
    return new FileResponse(roleFileRepository.save(file), null);
  }

  @Transactional
  public FileResponse uploadFile(
      Long roleId, String parentDir, String originalFilename, byte[] content, Long userId) {
    Long projectId = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkMembership(projectId, userId);
    if (content != null && content.length > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds 500MB limit");
    }
    String dir = parentDir != null ? parentDir : "";
    if (roleFileRepository.existsByRoleIdAndParentDirAndName(roleId, dir, originalFilename)) {
      throw new IllegalArgumentException(
          "File or directory '" + originalFilename + "' already exists");
    }
    RoleFile file = new RoleFile();
    file.setRoleId(roleId);
    file.setParentDir(dir);
    file.setName(originalFilename);
    file.setIsDirectory(false);
    file.setContent(content != null ? content : new byte[0]);
    file.setCreatedBy(userId);
    return new FileResponse(roleFileRepository.save(file), null);
  }

  @Transactional(readOnly = true)
  public List<FileResponse> listFilesAsTree(Long roleId, Long userId) {
    Long projectId = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkMembership(projectId, userId);
    List<RoleFile> allFiles = roleFileRepository.findByRoleIdOrderByParentDirAscNameAsc(roleId);
    Map<String, List<RoleFile>> byParent =
        allFiles.stream().collect(Collectors.groupingBy(RoleFile::getParentDir));

    return buildTree("", byParent);
  }

  private List<FileResponse> buildTree(
      String parentDir, Map<String, List<RoleFile>> byParent) {
    List<RoleFile> files = byParent.getOrDefault(parentDir, List.of());
    return files.stream()
        .map(
            f ->
                new FileResponse(
                    f,
                    buildTree(
                        f.getParentDir().isEmpty()
                            ? f.getName()
                            : f.getParentDir() + "/" + f.getName(),
                        byParent)))
        .toList();
  }

  @Transactional(readOnly = true)
  public FileResponse getFile(Long fileId, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    Long projectId = roleRepository.findById(file.getRoleId())
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkMembership(projectId, userId);
    return new FileResponse(file, null);
  }

  @Transactional(readOnly = true)
  public byte[] getFileContent(Long fileId, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (Boolean.TRUE.equals(file.getIsDirectory())) {
      throw new IllegalArgumentException("Cannot download a directory");
    }
    Role role =
        roleRepository
            .findById(file.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), userId);
    return file.getContent();
  }

  @Transactional(readOnly = true)
  public String getFileName(Long fileId, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    Role role =
        roleRepository
            .findById(file.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), userId);
    return file.getName();
  }

  @Transactional
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  public FileResponse updateFile(Long fileId, UpdateFileRequest request, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    Long projectId = roleRepository.findById(file.getRoleId())
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkOwnerOrAdmin(projectId, file.getCreatedBy(), userId);

    String newParentDir = request.getParentDir();
    String newName = request.getName();
    boolean parentDirChanged =
        newParentDir != null && !Objects.equals(newParentDir, file.getParentDir());
    boolean nameChanged =
        StringUtils.hasText(newName) && !newName.equals(file.getName());

    if (parentDirChanged || nameChanged) {
      String checkDir = parentDirChanged ? newParentDir : file.getParentDir();
      String checkName = nameChanged ? newName : file.getName();
      if (roleFileRepository.existsByRoleIdAndParentDirAndName(
          file.getRoleId(), checkDir, checkName)) {
        throw new IllegalArgumentException(
            "File or directory '" + checkName + "' already exists");
      }
      if (parentDirChanged) {
        file.setParentDir(newParentDir);
      }
      if (nameChanged) {
        file.setName(newName);
      }
    }

    if (request.getTargetPath() != null) {
      file.setTargetPath(request.getTargetPath());
    }
    if (request.getTextContent() != null && !Boolean.TRUE.equals(file.getIsDirectory())) {
      file.setContent(request.getTextContent().getBytes(StandardCharsets.UTF_8));
    }
    return new FileResponse(roleFileRepository.save(file), null);
  }

  @Transactional
  public void deleteFile(Long fileId, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    Long projectId = roleRepository.findById(file.getRoleId())
        .orElseThrow(() -> new IllegalArgumentException("Role not found")).getProjectId();
    accessChecker.checkOwnerOrAdmin(projectId, file.getCreatedBy(), userId);
    if (Boolean.TRUE.equals(file.getIsDirectory())) {
      String childPrefix =
          file.getParentDir().isEmpty()
              ? file.getName() + "/"
              : file.getParentDir() + "/" + file.getName() + "/";
      List<RoleFile> children =
          roleFileRepository.findByRoleIdAndParentDirStartingWith(file.getRoleId(), childPrefix);
      roleFileRepository.deleteAll(children);
    }
    roleFileRepository.delete(file);
  }
}
