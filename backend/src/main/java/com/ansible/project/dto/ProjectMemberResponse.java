package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.user.entity.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProjectMemberResponse {

  private final Long userId;
  private final String username;
  private final String email;
  private final ProjectRole role;
  private final LocalDateTime joinedAt;

  @JsonCreator
  public ProjectMemberResponse(
      @JsonProperty("userId") Long userId,
      @JsonProperty("username") String username,
      @JsonProperty("email") String email,
      @JsonProperty("role") ProjectRole role,
      @JsonProperty("joinedAt") LocalDateTime joinedAt) {
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.role = role;
    this.joinedAt = joinedAt;
  }

  public ProjectMemberResponse(ProjectMember member, User user) {
    this.userId = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.role = member.getRole();
    this.joinedAt = member.getJoinedAt();
  }
}
