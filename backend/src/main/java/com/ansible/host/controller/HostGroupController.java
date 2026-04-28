package com.ansible.host.controller;

import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.service.HostGroupService;
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
public class HostGroupController {

  private final HostGroupService hostGroupService;

  @PostMapping("/projects/{projectId}/host-groups")
  public Result<HostGroupResponse> createHostGroup(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateHostGroupRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.createHostGroup(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/host-groups")
  public Result<List<HostGroupResponse>> getHostGroups(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.getHostGroupsByProject(projectId, currentUserId));
  }

  @GetMapping("/host-groups/{id}")
  public Result<HostGroupResponse> getHostGroup(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.getHostGroup(id, currentUserId));
  }

  @PutMapping("/host-groups/{id}")
  public Result<HostGroupResponse> updateHostGroup(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHostGroupRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.updateHostGroup(id, request, currentUserId));
  }

  @DeleteMapping("/host-groups/{id}")
  public Result<Void> deleteHostGroup(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    hostGroupService.deleteHostGroup(id, currentUserId);
    return Result.success();
  }

  @PostMapping("/host-groups/{id}/copy")
  public Result<HostGroupResponse> copyHostGroup(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.copyHostGroup(id, currentUserId));
  }
}
