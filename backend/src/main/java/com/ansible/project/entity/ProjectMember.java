package com.ansible.project.entity;

import com.ansible.common.enums.ProjectRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"projectId", "userId"}),
    indexes = {@Index(name = "idx_project_member_user_id", columnList = "user_id")})
@Getter
@Setter
@NoArgsConstructor
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectRole role;

  @Column(nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @PrePersist
  void onCreate() {
    joinedAt = LocalDateTime.now();
  }
}
