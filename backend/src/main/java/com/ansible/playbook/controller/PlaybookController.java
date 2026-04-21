package com.ansible.playbook.controller;

import com.ansible.common.Result;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookResponse;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.dto.UpdatePlaybookRequest;
import com.ansible.playbook.service.PlaybookService;
import jakarta.validation.Valid;
import java.util.List;
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
@SuppressWarnings("PMD.TooManyMethods")
public class PlaybookController {

  private final PlaybookService playbookService;

  public PlaybookController(PlaybookService playbookService) {
    this.playbookService = playbookService;
  }

  @PostMapping("/projects/{projectId}/playbooks")
  public Result<PlaybookResponse> createPlaybook(
      @PathVariable Long projectId,
      @Valid @RequestBody CreatePlaybookRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(playbookService.createPlaybook(projectId, request, userId));
  }

  @GetMapping("/projects/{projectId}/playbooks")
  public Result<List<PlaybookResponse>> listPlaybooks(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(playbookService.listPlaybooks(projectId, userId));
  }

  @GetMapping("/playbooks/{playbookId}")
  public Result<PlaybookResponse> getPlaybook(
      @PathVariable Long playbookId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(playbookService.getPlaybook(playbookId, userId));
  }

  @PutMapping("/playbooks/{playbookId}")
  public Result<PlaybookResponse> updatePlaybook(
      @PathVariable Long playbookId,
      @Valid @RequestBody UpdatePlaybookRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(playbookService.updatePlaybook(playbookId, request, userId));
  }

  @DeleteMapping("/playbooks/{playbookId}")
  public Result<Void> deletePlaybook(
      @PathVariable Long playbookId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.deletePlaybook(playbookId, userId);
    return Result.success();
  }

  @PostMapping("/playbooks/{playbookId}/roles")
  public Result<Void> addRole(
      @PathVariable Long playbookId,
      @Valid @RequestBody PlaybookRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.addRole(playbookId, request, userId);
    return Result.success();
  }

  @DeleteMapping("/playbooks/{playbookId}/roles/{roleId}")
  public Result<Void> removeRole(
      @PathVariable Long playbookId,
      @PathVariable Long roleId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.removeRole(playbookId, roleId, userId);
    return Result.success();
  }

  @PutMapping("/playbooks/{playbookId}/roles/order")
  public Result<Void> reorderRoles(
      @PathVariable Long playbookId,
      @RequestBody List<Long> roleIds,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.reorderRoles(playbookId, roleIds, userId);
    return Result.success();
  }

  @PostMapping("/playbooks/{playbookId}/host-groups/{hostGroupId}")
  public Result<Void> addHostGroup(
      @PathVariable Long playbookId,
      @PathVariable Long hostGroupId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.addHostGroup(playbookId, hostGroupId, userId);
    return Result.success();
  }

  @DeleteMapping("/playbooks/{playbookId}/host-groups/{hostGroupId}")
  public Result<Void> removeHostGroup(
      @PathVariable Long playbookId,
      @PathVariable Long hostGroupId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.removeHostGroup(playbookId, hostGroupId, userId);
    return Result.success();
  }

  @PostMapping("/playbooks/{playbookId}/tags/{tagId}")
  public Result<Void> addTag(
      @PathVariable Long playbookId,
      @PathVariable Long tagId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.addTag(playbookId, tagId, userId);
    return Result.success();
  }

  @DeleteMapping("/playbooks/{playbookId}/tags/{tagId}")
  public Result<Void> removeTag(
      @PathVariable Long playbookId,
      @PathVariable Long tagId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    playbookService.removeTag(playbookId, tagId, userId);
    return Result.success();
  }

  @GetMapping("/playbooks/{playbookId}/yaml")
  public Result<String> generateYaml(
      @PathVariable Long playbookId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(playbookService.generateYaml(playbookId, userId));
  }
}
