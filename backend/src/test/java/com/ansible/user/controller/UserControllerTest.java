package com.ansible.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UserControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;

  private String token;
  private Long userId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = response.getBody().getData().getToken();
    userId = response.getBody().getData().getUser().getId();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Test
  void getUser_success() {
    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("alice");
  }

  @Test
  void getUser_unauthorized_without_token() {
    ResponseEntity<Object> response =
        restTemplate.getForEntity("/api/users/" + userId, Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void updateUser_email_success() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("newalice@example.com");

    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.PUT,
            new HttpEntity<>(request, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getEmail()).isEqualTo("newalice@example.com");
  }

  @Test
  void deleteUser_success() {
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(userRepository.findById(userId)).isEmpty();
  }
}