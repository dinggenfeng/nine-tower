package com.ansible.common;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class EncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final SecretKeySpec secretKey;

  public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    if (keyBytes.length != 32) {
      throw new IllegalStateException("AES-256 key must be 32 bytes");
    }
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String plaintext) {
    if (!StringUtils.hasText(plaintext)) {
      return plaintext;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
      byte[] ciphertext =
          cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Encryption failed", e);
    }
  }

  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.equals(mask())) {
      return encrypted;
    }
    try {
      byte[] combined = Base64.getDecoder().decode(encrypted);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, iv.length);
      System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
      return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new EncryptionException("Decryption failed", e);
    }
  }

  public static String mask() {
    return "****";
  }
}
