package com.ccds.infra.hazmat;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.dto.HazmatSearchResultDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * 危化品检索占位。未配置应急管理部库时拒绝，禁止返回假条目。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class HazmatSearchClientImpl implements HazmatSearchClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public HazmatSearchResultDTO search(String query, int limit) {
        log.warn("危化品检索未配置外部库");
        throw new IllegalStateException("hazmat search not configured");
    }
}
