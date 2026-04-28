package com.ansible.variable.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "variables",
    uniqueConstraints = @UniqueConstraint(columnNames = {"scope", "scope_id", "key"}))
@Getter
@Setter
@NoArgsConstructor
public class Variable extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VariableScope scope;

  @Column(name = "scope_id", nullable = false)
  private Long scopeId;

  @Column(nullable = false, length = 200)
  private String key;

  @Column(columnDefinition = "TEXT")
  private String value;
}
