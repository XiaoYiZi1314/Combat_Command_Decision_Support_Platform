package com.ccds.assist.assist.service;

import com.ccds.assist.assist.dto.HazmatSearchResultDTO;
import com.ccds.assist.assist.dto.HazmatVisionCommand;
import com.ccds.assist.assist.dto.HazmatVisionResultDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 危化品检索与视觉/文字研判。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface HazmatService {

    /**
     * 按名称 / 俗称 / UN / CAS 检索。
     *
     * @param principal 当前身份
     * @param query     检索词
     * @return 检索结果
     */
    HazmatSearchResultDTO search(AuthPrincipal principal, String query);

    /**
     * 拍照 / 图库 / 文字研判。图只送到本后端。
     *
     * @param principal 当前身份
     * @param command   图+文
     * @return 研判结果
     */
    HazmatVisionResultDTO vision(AuthPrincipal principal, HazmatVisionCommand command);
}
