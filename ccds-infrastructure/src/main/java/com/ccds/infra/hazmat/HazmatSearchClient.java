package com.ccds.infra.hazmat;

import com.ccds.assist.assist.dto.HazmatSearchResultDTO;

/**
 * 危化品检索客户端。后端转发或缓存，APP 不直连外链。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface HazmatSearchClient {

    /**
     * 按名称 / 俗称 / UN / CAS 检索。未配置时抛非法状态。
     *
     * @param query 检索词
     * @param limit 条数上限
     * @return 检索结果
     */
    HazmatSearchResultDTO search(String query, int limit);
}
