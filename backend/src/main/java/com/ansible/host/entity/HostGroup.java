package com.ansible.host.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "host_groups")
@Getter
@Setter
@NoArgsConstructor
public class HostGroup extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;
}
