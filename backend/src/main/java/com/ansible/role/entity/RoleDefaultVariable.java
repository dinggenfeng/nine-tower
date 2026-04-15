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
    name = "role_default_variables",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "variable_key"}))
@Getter
@Setter
@NoArgsConstructor
public class RoleDefaultVariable extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "variable_key", nullable = false, length = 200)
  private String key;

  @Column(columnDefinition = "TEXT")
  private String value;
}
