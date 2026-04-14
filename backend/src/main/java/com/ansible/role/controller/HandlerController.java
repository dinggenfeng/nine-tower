package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.service.HandlerService;
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
public class HandlerController {

  private final HandlerService handlerService;

  @PostMapping("/roles/{roleId}/handlers")
  public Result<HandlerResponse> createHandler(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateHandlerRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.createHandler(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/handlers")
  public Result<List<HandlerResponse>> getHandlers(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.getHandlersByRole(roleId, currentUserId));
  }

  @GetMapping("/handlers/{id}")
  public Result<HandlerResponse> getHandler(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.getHandler(id, currentUserId));
  }

  @PutMapping("/handlers/{id}")
  public Result<HandlerResponse> updateHandler(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHandlerRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.updateHandler(id, request, currentUserId));
  }

  @DeleteMapping("/handlers/{id}")
  public Result<Void> deleteHandler(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    handlerService.deleteHandler(id, currentUserId);
    return Result.success();
  }
}
