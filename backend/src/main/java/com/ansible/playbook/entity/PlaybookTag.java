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
    name = "playbook_tags",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"playbookId", "tagId"})},
    indexes = {@Index(name = "idx_playbook_tag_tag_id", columnList = "tag_id")})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookTag extends BaseEntity {

  @Column(nullable = false)
  private Long playbookId;

  @Column(nullable = false)
  private Long tagId;
}
