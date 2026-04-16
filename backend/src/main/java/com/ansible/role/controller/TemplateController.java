package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemplateController {

  private final TemplateService templateService;

  @PostMapping("/roles/{roleId}/templates")
  public Result<TemplateResponse> createTemplate(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateTemplateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.createTemplate(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/templates")
  public Result<List<TemplateResponse>> getTemplates(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.getTemplatesByRole(roleId, currentUserId));
  }

  @GetMapping("/templates/{id}")
  public Result<TemplateResponse> getTemplate(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.getTemplate(id, currentUserId));
  }

  @PutMapping("/templates/{id}")
  public Result<TemplateResponse> updateTemplate(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTemplateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.updateTemplate(id, request, currentUserId));
  }

  @DeleteMapping("/templates/{id}")
  public Result<Void> deleteTemplate(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    templateService.deleteTemplate(id, currentUserId);
    return Result.success();
  }
}
