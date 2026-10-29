package com.ccds.infra.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 版本与更新配置。由后端配置驱动，前端不再直连云端清单。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Component
@ConfigurationProperties(prefix = "ccds.app")
public class AppVersionProperties {

    /**
     * 当前版本名，如 0.1.1。
     */
    private String versionName = "0.1.1";

    /**
     * 当前版本编号，用于与客户端比较是否为最新版。
     */
    private Integer versionCode = Integer.valueOf(2);

    /**
     * 更新说明。
     */
    private String releaseNotes = "";

    /**
     * 更新页地址，需为 https。
     */
    private String updatePageUrl = "";

    /**
     * 安装教程地址。
     */
    private String installGuideUrl = "";

    /**
     * 操作教程地址。
     */
    private String operationGuideUrl = "";

    /**
     * 是否强制更新。
     */
    private Boolean forceUpdate = Boolean.FALSE;
}
