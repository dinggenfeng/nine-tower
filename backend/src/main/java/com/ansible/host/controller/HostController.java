package com.ansible.host.controller;

import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.service.HostService;
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
public class HostController {

  private final HostService hostService;

  @PostMapping("/host-groups/{hgId}/hosts")
  public Result<HostResponse> createHost(
      @PathVariable Long hgId,
      @Valid @RequestBody CreateHostRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.createHost(hgId, request, currentUserId));
  }

  @GetMapping("/host-groups/{hgId}/hosts")
  public Result<List<HostResponse>> getHosts(
      @PathVariable Long hgId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.getHostsByHostGroup(hgId, currentUserId));
  }

  @GetMapping("/hosts/{id}")
  public Result<HostResponse> getHost(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.getHost(id, currentUserId));
  }

  @PutMapping("/hosts/{id}")
  public Result<HostResponse> updateHost(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHostRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.updateHost(id, request, currentUserId));
  }

  @DeleteMapping("/hosts/{id}")
  public Result<Void> deleteHost(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    hostService.deleteHost(id, currentUserId);
    return Result.success();
  }
}
