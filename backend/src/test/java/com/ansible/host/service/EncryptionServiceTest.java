package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

  private EncryptionService encryptionService;

  @BeforeEach
  void setUp() {
    encryptionService = new EncryptionService("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
  }

  @Test
  void encrypt_then_decrypt_returns_original() {
    String original = "my-secret-password";
    String encrypted = encryptionService.encrypt(original);
    assertThat(encrypted).isNotEqualTo(original);
    assertThat(encryptionService.decrypt(encrypted)).isEqualTo(original);
  }

  @Test
  void encrypt_produces_different_ciphertext_each_time() {
    String plaintext = "password";
    String encrypted1 = encryptionService.encrypt(plaintext);
    String encrypted2 = encryptionService.encrypt(plaintext);
    assertThat(encrypted1).isNotEqualTo(encrypted2);
  }

  @Test
  void encrypt_null_returns_null() {
    assertThat(encryptionService.encrypt(null)).isNull();
  }

  @Test
  void encrypt_empty_returns_empty() {
    assertThat(encryptionService.encrypt("")).isEmpty();
  }

  @Test
  void decrypt_mask_returns_mask() {
    assertThat(encryptionService.decrypt("****")).isEqualTo("****");
  }

  @Test
  void decrypt_null_returns_null() {
    assertThat(encryptionService.decrypt(null)).isNull();
  }

  @Test
  void mask_returns_four_asterisks() {
    assertThat(EncryptionService.mask()).isEqualTo("****");
  }
}
