package com.ansible.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void register_returns_token() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("alice");
    request.setPassword("password123");
    request.setEmail("alice@example.com");

    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getToken()).isNotBlank();
    assertThat(response.getBody().getData().getUser().getUsername()).isEqualTo("alice");
  }

  @Test
  void register_fails_duplicate_username() {
    RegisterRequest first = new RegisterRequest();
    first.setUsername("alice");
    first.setPassword("password123");
    first.setEmail("alice@example.com");
    restTemplate.postForEntity("/api/auth/register", first, Object.class);

    RegisterRequest second = new RegisterRequest();
    second.setUsername("alice");
    second.setPassword("password456");
    second.setEmail("other@example.com");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(second),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_returns_token() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("bob");
    reg.setPassword("password123");
    reg.setEmail("bob@example.com");
    restTemplate.postForEntity("/api/auth/register", reg, Object.class);

    LoginRequest login = new LoginRequest();
    login.setUsername("bob");
    login.setPassword("password123");

    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(login),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getToken()).isNotBlank();
  }

  @Test
  void login_fails_wrong_password() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("charlie");
    reg.setPassword("password123");
    reg.setEmail("charlie@example.com");
    restTemplate.postForEntity("/api/auth/register", reg, Object.class);

    LoginRequest login = new LoginRequest();
    login.setUsername("charlie");
    login.setPassword("wrongpassword");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(login),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void logout_returns_success() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("eve");
    reg.setPassword("password123");
    reg.setEmail("eve@example.com");
    ResponseEntity<Result<TokenResponse>> regResponse =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    String token = regResponse.getBody().getData().getToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/auth/logout",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void me_returns_current_user() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("dave");
    reg.setPassword("password123");
    reg.setEmail("dave@example.com");
    ResponseEntity<Result<TokenResponse>> regResponse =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    String token = regResponse.getBody().getData().getToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("dave");
  }
}
