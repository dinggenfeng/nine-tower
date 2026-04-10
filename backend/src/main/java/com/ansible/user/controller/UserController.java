package com.ansible.user.controller;

import com.ansible.common.Result;
import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public Result<Page<UserResponse>> searchUsers(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return Result.success(userService.searchUsers(keyword, pageable));
  }

  @GetMapping("/{id}")
  public Result<UserResponse> getUser(@PathVariable Long id) {
    return Result.success(userService.getUser(id));
  }

  @PutMapping("/{id}")
  public Result<UserResponse> updateUser(
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(userService.updateUser(id, request, currentUserId));
  }

  @DeleteMapping("/{id}")
  public Result<Void> deleteUser(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    userService.deleteUser(id, currentUserId);
    return Result.success();
  }
}