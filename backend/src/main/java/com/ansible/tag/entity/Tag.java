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
@Table(name = "tags", uniqueConstraints = {@UniqueConstraint(columnNames = {"projectId", "name"})})
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 100)
  private String name;
}
