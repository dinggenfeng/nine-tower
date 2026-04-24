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
    name = "playbook_host_groups",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"playbookId", "hostGroupId"})},
    indexes = {@Index(name = "idx_playbook_host_group_host_group_id", columnList = "host_group_id")})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookHostGroup extends BaseEntity {

  @Column(nullable = false)
  private Long playbookId;

  @Column(nullable = false)
  private Long hostGroupId;
}
