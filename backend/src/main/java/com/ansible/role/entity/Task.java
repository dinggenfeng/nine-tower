package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task extends BaseEntity {

  @Column(nullable = false)
  private Long roleId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 100)
  private String module;

  @Column(columnDefinition = "TEXT")
  private String args;

  @Column(name = "when_condition", length = 500)
  private String whenCondition;

  @Column(length = 500)
  private String loop;

  @Column(length = 500)
  private String until;

  @Column(length = 100)
  private String register;

  @Column(columnDefinition = "TEXT")
  private String notify;

  @Column(nullable = false)
  private Integer taskOrder;

  @Column(name = "become_flag")
  private Boolean become;

  @Column(name = "become_user", length = 100)
  private String becomeUser;

  @Column(name = "ignore_errors")
  private Boolean ignoreErrors;
}
