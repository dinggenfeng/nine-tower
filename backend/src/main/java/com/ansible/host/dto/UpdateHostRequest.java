package com.ansible.host.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostRequest {

  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 45, message = "IP must not exceed 45 characters")
  private String ip;

  private Integer port;

  @Size(max = 100, message = "SSH user must not exceed 100 characters")
  private String ansibleUser;

  @Size(max = 500, message = "SSH password must not exceed 500 characters")
  private String ansibleSshPass;

  @Size(max = 2000, message = "SSH private key must not exceed 2000 characters")
  private String ansibleSshPrivateKeyFile;

  private Boolean ansibleBecome;
}