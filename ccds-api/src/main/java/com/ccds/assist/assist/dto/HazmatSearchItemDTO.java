package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 危化品检索命中项。由后端转发或缓存，APP 不直连外链。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HazmatSearchItemDTO {

    /**
     * 名称。
     */
    private String name;

    /**
     * 俗称。
     */
    private String alias;

    /**
     * UN 编号。
     */
    private String unNumber;

    /**
     * CAS 号。
     */
    private String casNumber;

    /**
     * 危险性摘要。
     */
    private String hazardSummary;

    /**
     * 处置要点。
     */
    private String responseHint;
}
