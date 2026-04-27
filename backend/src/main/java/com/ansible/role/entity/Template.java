package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "templates",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"role_id", "parent_dir", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Template extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "parent_dir", length = 500)
  private String parentDir;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "target_path", length = 500)
  private String targetPath;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_directory")
  private Boolean isDirectory = false;
}
