package com.ansible.role.service;

import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.entity.RoleFile;
import com.ansible.role.repository.RoleFileRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleFileService {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  private final RoleFileRepository roleFileRepository;

  @Transactional
  public FileResponse createFile(
      Long roleId, CreateFileRequest request, byte[] content, Long userId) {
    boolean isDir = Boolean.TRUE.equals(request.getIsDirectory());
    if (roleFileRepository.existsByRoleIdAndParentDirAndName(
        roleId, request.getParentDir(), request.getName())) {
      throw new IllegalArgumentException(
          "File or directory '" + request.getName() + "' already exists");
    }
    if (!isDir && content != null && content.length > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds 10MB limit");
    }
    RoleFile file = new RoleFile();
    file.setRoleId(roleId);
    file.setParentDir(request.getParentDir());
    file.setName(request.getName());
    file.setIsDirectory(isDir);
    file.setContent(isDir ? null : content != null ? content : new byte[0]);
    file.setCreatedBy(userId);
    return new FileResponse(roleFileRepository.save(file), null);
  }

  @Transactional(readOnly = true)
  public List<FileResponse> listFilesAsTree(Long roleId) {
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
  public FileResponse getFile(Long fileId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    return new FileResponse(file, null);
  }

  @Transactional(readOnly = true)
  public byte[] getFileContent(Long fileId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (Boolean.TRUE.equals(file.getIsDirectory())) {
      throw new IllegalArgumentException("Cannot download a directory");
    }
    return file.getContent();
  }

  @Transactional(readOnly = true)
  public String getFileName(Long fileId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    return file.getName();
  }

  @Transactional
  public FileResponse updateFile(Long fileId, UpdateFileRequest request, Long userId) {
    RoleFile file =
        roleFileRepository
            .findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (!file.getName().equals(request.getName())
        && roleFileRepository.existsByRoleIdAndParentDirAndNameAndIdNot(
            file.getRoleId(), file.getParentDir(), request.getName(), fileId)) {
      throw new IllegalArgumentException(
          "File or directory '" + request.getName() + "' already exists");
    }
    file.setName(request.getName());
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
