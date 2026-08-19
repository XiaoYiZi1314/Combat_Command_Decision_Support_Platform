package com.ccds.iam.identity.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可见大队及其下属站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrigadeVO {

    /**
     * 大队主键，支队直属节点为 null。
     */
    private Long id;

    /**
     * 编制编码。
     */
    private String code;

    /**
     * 大队名称。
     */
    private String name;

    /**
     * 下属站。
     */
    private List<StationVO> stations;
}
