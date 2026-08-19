package com.ccds.org.org.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消防站编制。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属大队，空为支队直属。
     */
    private Long brigadeId;

    /**
     * 站名。
     */
    private String name;

    /**
     * 归一站名。
     */
    private String nameNorm;

    /**
     * 站类别。
     */
    private String category;

    /**
     * 展示顺序。
     */
    private Integer sortNo;

    /**
     * 所属大队名称（联查）。
     */
    private String brigadeName;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
