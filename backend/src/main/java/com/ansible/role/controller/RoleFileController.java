package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.service.RoleFileService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleFileController {

  private final RoleFileService roleFileService;

  @PostMapping("/roles/{roleId}/files")
  public Result<FileResponse> createFile(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateFileRequest request,
      @RequestParam(required = false) String textContent,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    String text = request.getTextContent() != null ? request.getTextContent() : textContent;
    byte[] content = text != null ? text.getBytes(StandardCharsets.UTF_8) : null;
    return Result.success(roleFileService.createFile(roleId, request, content, currentUserId));
  }

  @PostMapping("/roles/{roleId}/files/upload")
  public Result<FileResponse> uploadFile(
      @PathVariable Long roleId,
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) String parentDir,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    try {
      byte[] content = file.getBytes();
      return Result.success(roleFileService.uploadFile(
          roleId, parentDir, file.getOriginalFilename(), content, currentUserId));
    } catch (java.io.IOException e) {
      throw new IllegalArgumentException("Failed to read uploaded file", e);
    }
  }

  @GetMapping("/roles/{roleId}/files")
  public Result<List<FileResponse>> listFiles(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleFileService.listFilesAsTree(roleId, currentUserId));
  }

  @GetMapping("/files/{fileId}")
  public Result<FileResponse> getFile(
      @PathVariable Long fileId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleFileService.getFile(fileId, currentUserId));
  }

  @PutMapping("/files/{fileId}")
  public Result<FileResponse> updateFile(
      @PathVariable Long fileId,
      @Valid @RequestBody UpdateFileRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleFileService.updateFile(fileId, request, currentUserId));
  }

  @DeleteMapping("/files/{fileId}")
  public Result<Void> deleteFile(
      @PathVariable Long fileId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleFileService.deleteFile(fileId, currentUserId);
    return Result.success();
  }

  @GetMapping("/files/{fileId}/download")
  public ResponseEntity<byte[]> downloadFile(
      @PathVariable Long fileId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    byte[] content = roleFileService.getFileContent(fileId, currentUserId);
    String filename = roleFileService.getFileName(fileId, currentUserId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(content);
  }
}
