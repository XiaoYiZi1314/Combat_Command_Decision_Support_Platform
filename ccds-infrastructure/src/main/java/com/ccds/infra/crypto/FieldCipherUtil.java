package com.ccds.infra.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 电话等敏感字段 AES-GCM 加解密。明文禁止入库、禁止打日志。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
public class FieldCipherUtil {

    private static final String PREFIX = "v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final String ALGORITHM = "AES";

    private static final int GCM_TAG_BITS = 128;

    private static final int IV_LENGTH = 12;

    private static final int KEY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FieldCipherProperties properties;

    /**
     * 加密明文。空值返回 null。
     *
     * @param plain 明文
     * @return 密文，空输入为 null
     */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        byte[] key = requireKey();
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("field encrypt failed", ex);
        }
    }

    /**
     * 解密密文。空值返回 null。
     *
     * @param cipherText 密文
     * @return 明文，空输入为 null
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return null;
        }
        if (!cipherText.startsWith(PREFIX)) {
            throw new IllegalStateException("field cipher version unsupported");
        }
        byte[] key = requireKey();
        byte[] packed = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
        if (packed.length <= IV_LENGTH) {
            throw new IllegalStateException("field cipher payload invalid");
        }
        ByteBuffer buffer = ByteBuffer.wrap(packed);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] body = new byte[buffer.remaining()];
        buffer.get(body);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("field decrypt failed", ex);
        }
    }

    private byte[] requireKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("CCDS_FIELD_SECRET missing");
        }
        String trimmed = secret.trim();
        byte[] key;
        if (trimmed.length() == KEY_BYTES * 2) {
            key = HexFormat.of().parseHex(trimmed);
        } else {
            key = Base64.getDecoder().decode(trimmed);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException("CCDS_FIELD_SECRET length invalid");
        }
        return key;
    }
}
