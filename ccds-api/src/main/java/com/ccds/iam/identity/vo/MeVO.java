package com.ccds.iam.identity.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录账号与可见站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeVO {

    /**
     * 账号主键。
     */
    private Long accountId;

    /**
     * 登录名。
     */
    private String username;

    /**
     * 角色码：station / brigade / hq / developer。
     */
    private String role;

    /**
     * 站级所属站。
     */
    private Long stationId;

    /**
     * 站级所属站名。
     */
    private String stationName;

    /**
     * 大队所属大队。
     */
    private Long brigadeId;

    /**
     * 大队所属大队名。
     */
    private String brigadeName;

    /**
     * 是否必须改密。
     */
    private Boolean mustChangePassword;

    /**
     * 登录后默认路由。
     */
    private String homeHash;

    /**
     * 可见站列表。
     */
    private List<StationVO> visibleStations;
}
