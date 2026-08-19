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
}
