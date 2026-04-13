package com.ansible.host.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHostRequest {

  @NotBlank(message = "Host name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @NotBlank(message = "IP address is required")
  @Size(max = 45, message = "IP must not exceed 45 characters")
  private String ip;

  private Integer port = 22;

  @Size(max = 100, message = "SSH user must not exceed 100 characters")
  private String ansibleUser;

  @Size(max = 500, message = "SSH password must not exceed 500 characters")
  private String ansibleSshPass;

  @Size(max = 2000, message = "SSH private key must not exceed 2000 characters")
  private String ansibleSshPrivateKeyFile;

  private Boolean ansibleBecome = false;
}