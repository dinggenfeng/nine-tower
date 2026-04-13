package com.ansible.host.dto;

import com.ansible.common.EncryptionService;
import com.ansible.host.entity.Host;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HostResponse {

  private final Long id;
  private final Long hostGroupId;
  private final String name;
  private final String ip;
  private final Integer port;
  private final String ansibleUser;
  private final String ansibleSshPass;
  private final String ansibleSshPrivateKeyFile;
  private final Boolean ansibleBecome;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  @SuppressWarnings("PMD.ExcessiveParameterList")
  public HostResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("hostGroupId") Long hostGroupId,
      @JsonProperty("name") String name,
      @JsonProperty("ip") String ip,
      @JsonProperty("port") Integer port,
      @JsonProperty("ansibleUser") String ansibleUser,
      @JsonProperty("ansibleSshPass") String ansibleSshPass,
      @JsonProperty("ansibleSshPrivateKeyFile") String ansibleSshPrivateKeyFile,
      @JsonProperty("ansibleBecome") Boolean ansibleBecome,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.hostGroupId = hostGroupId;
    this.name = name;
    this.ip = ip;
    this.port = port;
    this.ansibleUser = ansibleUser;
    this.ansibleSshPass = ansibleSshPass;
    this.ansibleSshPrivateKeyFile = ansibleSshPrivateKeyFile;
    this.ansibleBecome = ansibleBecome;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public HostResponse(Host host) {
    this.id = host.getId();
    this.hostGroupId = host.getHostGroupId();
    this.name = host.getName();
    this.ip = host.getIp();
    this.port = host.getPort();
    this.ansibleUser = host.getAnsibleUser();
    this.ansibleSshPass = EncryptionService.mask();
    this.ansibleSshPrivateKeyFile = EncryptionService.mask();
    this.ansibleBecome = host.getAnsibleBecome();
    this.createdBy = host.getCreatedBy();
    this.createdAt = host.getCreatedAt();
  }
}