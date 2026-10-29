package com.ccds.ops.version.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本与更新信息。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionVO {

    /**
     * 软件名称。
     */
    private String appName;

    /**
     * 当前版本名。
     */
    private String versionName;

    /**
     * 当前版本编号。
     */
    private Integer versionCode;

    /**
     * 更新说明。
     */
    private String releaseNotes;

    /**
     * 更新页地址。
     */
    private String updatePageUrl;

    /**
     * 安装教程地址。
     */
    private String installGuideUrl;

    /**
     * 操作教程地址。
     */
    private String operationGuideUrl;

    /**
     * 是否强制更新。
     */
    private Boolean force;
}
