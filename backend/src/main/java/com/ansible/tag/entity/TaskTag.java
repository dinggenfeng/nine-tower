package com.ansible.tag.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_tags", uniqueConstraints = {@UniqueConstraint(columnNames = {"taskId", "tagId"})})
@Getter
@Setter
@NoArgsConstructor
public class TaskTag extends BaseEntity {

  @Column(nullable = false)
  private Long taskId;

  @Column(nullable = false)
  private Long tagId;
}
