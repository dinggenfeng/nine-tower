package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "playbook_roles",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"playbookId", "roleId"})},
    indexes = {@Index(name = "idx_playbook_role_role_id", columnList = "role_id")})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookRole extends BaseEntity {

  @Column(nullable = false)
  private Long playbookId;

  @Column(nullable = false)
  private Long roleId;

  @Column(nullable = false)
  private Integer orderIndex;
}
