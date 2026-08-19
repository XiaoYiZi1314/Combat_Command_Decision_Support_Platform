package com.ccds.iam.identity.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前账号可见编制树。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeVO {

    /**
     * 可见大队。
     */
    private List<BrigadeVO> brigades;

    /**
     * 支队直属站（如战勤保障分队）。
     */
    private List<StationVO> directStations;
}
