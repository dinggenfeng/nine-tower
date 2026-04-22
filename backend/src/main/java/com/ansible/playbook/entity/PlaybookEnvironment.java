package com.ansible.playbook.entity;

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
    name = "playbook_environments",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"playbookId", "environmentId"})})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookEnvironment extends BaseEntity {

  @Column(nullable = false)
  private Long playbookId;

  @Column(nullable = false)
  private Long environmentId;
}
