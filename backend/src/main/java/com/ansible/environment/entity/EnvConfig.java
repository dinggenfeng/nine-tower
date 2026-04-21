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
    name = "env_configs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"environmentId", "configKey"}))
@Getter
@Setter
@NoArgsConstructor
public class EnvConfig extends BaseEntity {

  @Column(nullable = false)
  private Long environmentId;

  @Column(nullable = false, length = 100)
  private String configKey;

  @Column(columnDefinition = "TEXT")
  private String configValue;
}
