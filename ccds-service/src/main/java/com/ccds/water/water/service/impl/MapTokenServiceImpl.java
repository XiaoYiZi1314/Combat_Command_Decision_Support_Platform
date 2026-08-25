package com.ccds.water.water.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.water.water.service.MapTokenService;
import com.ccds.water.water.vo.BaiduMapTokenVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 下发浏览器端百度地图 AK。AK 只在服务端配置，经此接口给登录后的前端。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class MapTokenServiceImpl implements MapTokenService {

    private static final String MSG_AK_MISSING = "地图未配置，请联系管理员";

    private final String baiduAk;

    public MapTokenServiceImpl(@Value("${ccds.map.baidu-ak:}") String baiduAk) {
        this.baiduAk = baiduAk;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaiduMapTokenVO baiduToken() {
        if (baiduAk == null || baiduAk.isBlank()) {
            throw new BizException(ErrorCodeConstant.MAP_AK_MISSING, MSG_AK_MISSING);
        }
        return BaiduMapTokenVO.builder().ak(baiduAk).build();
    }
}
