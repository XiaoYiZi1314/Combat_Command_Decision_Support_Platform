package com.ccds.water.water.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.infra.map.MapProperties;
import com.ccds.water.water.service.MapTokenService;
import com.ccds.water.water.vo.BaiduMapTokenVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 下发浏览器端百度地图 AK。AK 只在服务端配置，经此接口给登录后的前端。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapTokenServiceImpl implements MapTokenService {

    private static final String MSG_AK_MISSING = "地图未配置，请联系管理员";

    private final MapProperties mapProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public BaiduMapTokenVO baiduToken() {
        String ak = mapProperties.getBaiduAk();
        if (ak == null || ak.isBlank()) {
            throw new BizException(ErrorCodeConstant.MAP_AK_MISSING, MSG_AK_MISSING);
        }
        return BaiduMapTokenVO.builder().ak(ak).build();
    }
}
