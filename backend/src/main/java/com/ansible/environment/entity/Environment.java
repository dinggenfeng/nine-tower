package com.ansible.environment.entity;

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
    name = "environments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"projectId", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Environment extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;
}
