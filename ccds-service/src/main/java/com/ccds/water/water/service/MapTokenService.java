package com.ccds.water.water.service;

import com.ccds.water.water.vo.BaiduMapTokenVO;

/**
 * 地图凭证下发。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface MapTokenService {

    /**
     * 浏览器端百度地图 AK。
     *
     * @return 凭证
     */
    BaiduMapTokenVO baiduToken();
}
