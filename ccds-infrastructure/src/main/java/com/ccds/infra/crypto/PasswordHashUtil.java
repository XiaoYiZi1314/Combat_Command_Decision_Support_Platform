package com.ccds.infra.crypto;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 口令哈希工具。密钥与口令明文不得入库、不得进仓库。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class PasswordHashUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordHashUtil() {
    }

    /**
     * 对口令做 BCrypt。
     *
     * @param rawPassword 明文口令，调用方保证不落日志
     * @return 哈希
     */
    public static String hash(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文与哈希是否匹配。
     *
     * @param rawPassword 明文口令，调用方保证不落日志
     * @param passwordHash 已存哈希
     * @return true 表示匹配
     */
    public static boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return ENCODER.matches(rawPassword, passwordHash);
    }
}
