package com.ccds.water.water.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.enums.WaterStatusEnum;
import com.ccds.water.water.enums.WaterTypeEnum;
import com.ccds.water.water.vo.WaterVO;

/**
 * DO 转 VO。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
public class WaterViewTranslator {

    /**
     * 转 VO。
     *
     * @param water       水源
     * @param stationName 站名
     * @return VO
     */
    public WaterVO toVo(WaterSourceDO water, String stationName) {
        WaterTypeEnum type = WaterTypeEnum.fromCode(water.getType());
        WaterStatusEnum status = WaterStatusEnum.fromCode(water.getStatus());
        return WaterVO.builder()
                .id(water.getId())
                .stationId(water.getStationId())
                .stationName(stationName)
                .name(water.getName())
                .type(water.getType())
                .typeLabel(type == null ? water.getType() : type.getLabel())
                .address(water.getAddress())
                .lng(water.getLng())
                .lat(water.getLat())
                .status(water.getStatus())
                .statusLabel(status == null ? water.getStatus() : status.getLabel())
                .notes(water.getExtraJson())
                .gmtModified(water.getGmtModified())
                .build();
    }

    /**
     * 批量转 VO。
     *
     * @param waters      水源列表
     * @param stationName 站名
     * @return VO 列表，不会为 null
     */
    public List<WaterVO> toVos(List<WaterSourceDO> waters, String stationName) {
        List<WaterVO> out = new ArrayList<WaterVO>();
        if (waters == null) {
            return out;
        }
        for (WaterSourceDO water : waters) {
            out.add(toVo(water, stationName));
        }
        return out;
    }
}
