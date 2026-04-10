package com.ansible.user.controller;

import com.ansible.common.Result;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.service.AuthService;
import com.ansible.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  @PostMapping("/register")
  public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
    return Result.success(authService.register(request));
  }

  @PostMapping("/login")
  public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return Result.success(authService.login(request));
  }

  @GetMapping("/me")
  public Result<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(userService.getUser(userId));
  }
}
