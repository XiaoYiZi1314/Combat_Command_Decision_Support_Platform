package com.ccds.duty.constant;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传白名单与上限。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class FileRuleConstant {

    /**
     * 预签名 URL 有效秒数。
     */
    public static final int PRESIGN_EXPIRE_SECONDS = 3600;

    /**
     * 单文件最大字节数。
     */
    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;

    private static final Set<String> BIZ_TYPES = Set.of(
            FileBizTypeConstant.WATER_PHOTO,
            FileBizTypeConstant.WATER_DOC,
            FileBizTypeConstant.KEYUNIT_PLAN,
            FileBizTypeConstant.KEYUNIT_FLOOR,
            FileBizTypeConstant.HAZMAT_IMAGE);

    private static final Map<String, Set<String>> MIME_WHITELIST = Map.of(
            FileBizTypeConstant.WATER_PHOTO, Set.of("image/jpeg", "image/png"),
            FileBizTypeConstant.WATER_DOC, Set.of("application/pdf"),
            FileBizTypeConstant.KEYUNIT_PLAN, Set.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"),
            FileBizTypeConstant.KEYUNIT_FLOOR, Set.of("application/pdf", "image/jpeg", "image/png"),
            FileBizTypeConstant.HAZMAT_IMAGE, Set.of("image/jpeg", "image/png"));

    private FileRuleConstant() {
    }

    /**
     * 是否已登记的业务类型。
     *
     * @param bizType 业务类型
     * @return 已登记为 true
     */
    public static boolean isAllowedBizType(String bizType) {
        return bizType != null && BIZ_TYPES.contains(bizType);
    }

    /**
     * 该业务类型允许的 MIME。
     *
     * @param bizType 业务类型
     * @return 白名单，不会为 null
     */
    public static Set<String> allowedMimeTypes(String bizType) {
        Set<String> allowed = MIME_WHITELIST.get(bizType);
        if (allowed == null) {
            return Collections.emptySet();
        }
        return allowed;
    }

    /**
     * MIME 是否在白名单。
     *
     * @param bizType     业务类型
     * @param contentType MIME
     * @return 允许为 true
     */
    public static boolean isAllowedMime(String bizType, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        return allowedMimeTypes(bizType).contains(contentType.trim().toLowerCase());
    }
}
