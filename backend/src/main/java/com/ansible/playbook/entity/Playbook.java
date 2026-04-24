package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playbooks", indexes = {@Index(name = "idx_playbook_project_id", columnList = "project_id")})
@Getter
@Setter
@NoArgsConstructor
public class Playbook extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(columnDefinition = "TEXT")
  private String extraVars;
}
