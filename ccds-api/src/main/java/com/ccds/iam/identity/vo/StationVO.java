package com.ccds.iam.identity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可见消防站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationVO {

    /**
     * 站主键。
     */
    private Long id;

    /**
     * 站名。
     */
    private String name;

    /**
     * 所属大队主键，支队直属为空。
     */
    private Long brigadeId;

    /**
     * 所属大队名称，支队直属为空。
     */
    private String brigadeName;

    /**
     * 站类别。
     */
    private String category;

    /**
     * 展示顺序。
     */
    private Integer sortNo;
}
