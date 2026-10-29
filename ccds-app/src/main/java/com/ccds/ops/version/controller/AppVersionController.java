package com.ccds.ops.version.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.infra.app.AppVersionProperties;
import com.ccds.ops.version.vo.AppVersionVO;

import lombok.RequiredArgsConstructor;

/**
 * 版本与更新信息接入。配置驱动，替代旧版前端直连云端清单。
 *
 * @author ccds
 * @since 0.1.2
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class AppVersionController {

    private static final String APP_NAME = "作战指挥辅助决策平台";

    private final AppVersionProperties appVersionProperties;

    /**
     * 版本与更新信息。登录前也可读，便于登录页提示是否最新。
     *
     * @return 版本信息
     */
    @GetMapping(ApiPathConstant.APP_VERSION)
    public ApiResultVO<AppVersionVO> version() {
        return ApiResultVO.ok(AppVersionVO.builder()
                .appName(APP_NAME)
                .versionName(appVersionProperties.getVersionName())
                .versionCode(appVersionProperties.getVersionCode())
                .releaseNotes(appVersionProperties.getReleaseNotes())
                .updatePageUrl(appVersionProperties.getUpdatePageUrl())
                .installGuideUrl(appVersionProperties.getInstallGuideUrl())
                .operationGuideUrl(appVersionProperties.getOperationGuideUrl())
                .force(appVersionProperties.getForceUpdate())
                .build());
    }
}
