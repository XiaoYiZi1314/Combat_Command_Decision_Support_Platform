package com.ccds.bootstrap.seed;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.infra.crypto.PasswordHashUtil;

/**
 * 将运维持有的一次性种子口令转为 BCrypt。明文不得入库、不得进仓库。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class SeedPasswordHashGenerator {

    private static final String MSG_MISSING_PASSWORD = "缺少环境变量 CCDS_SEED_PASSWORD";

    private SeedPasswordHashGenerator() {
    }

    /**
     * 生成写入 {@code ccds_account.password_hash} 的哈希。
     *
     * @param rawPassword 环境变量中的一次性口令
     * @return BCrypt 哈希
     */
    public static String generateHash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BizException(ErrorCodeConstant.SYSTEM_ERROR, MSG_MISSING_PASSWORD);
        }
        return PasswordHashUtil.hash(rawPassword);
    }
}
