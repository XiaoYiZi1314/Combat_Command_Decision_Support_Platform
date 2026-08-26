package com.ccds.assist.assist.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 危化品检索结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HazmatSearchResultDTO {

    /**
     * 检索词。
     */
    private String query;

    /**
     * 命中条数。
     */
    private Integer count;

    /**
     * 命中列表，不会为 null。
     */
    private List<HazmatSearchItemDTO> items;
}
