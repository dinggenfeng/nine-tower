package com.ansible.host.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "hosts",
    indexes = {@Index(name = "idx_host_host_group_id", columnList = "host_group_id")})
@Getter
@Setter
@NoArgsConstructor
public class Host extends BaseEntity {

  @Column(nullable = false)
  private Long hostGroupId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 45)
  private String ip;

  @Column(nullable = false)
  private Integer port = 22;

  @Column(length = 100)
  private String ansibleUser;

  @Column(length = 500)
  private String ansibleSshPass;

  @Column(length = 2000)
  private String ansibleSshPrivateKeyFile;

  @Column(nullable = false)
  private Boolean ansibleBecome = false;
}