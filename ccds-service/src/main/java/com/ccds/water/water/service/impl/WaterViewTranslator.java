package com.ccds.water.water.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.enums.WaterPartEnum;
import com.ccds.water.water.enums.WaterReasonEnum;
import com.ccds.water.water.enums.WaterSetupEnum;
import com.ccds.water.water.enums.WaterStatusEnum;
import com.ccds.water.water.enums.WaterSupplyFormEnum;
import com.ccds.water.water.enums.WaterTypeEnum;
import com.ccds.water.water.enums.WaterUsableEnum;
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
        WaterSetupEnum setup = WaterSetupEnum.fromCode(water.getSetup());
        WaterSupplyFormEnum supplyForm = WaterSupplyFormEnum.fromCode(water.getSupplyForm());
        WaterUsableEnum usable = WaterUsableEnum.fromCode(water.getUsable());
        WaterReasonEnum reason = WaterReasonEnum.fromCode(water.getReasonCode());
        WaterPartEnum part = WaterPartEnum.fromCode(water.getPartCode());
        boolean bad = WaterStatusResolver.isBad(water.getStatus(), water.getUsable(), water.getIsRepair());
        return WaterVO.builder()
                .id(water.getId())
                .waterNo(water.getWaterNo())
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
                .bad(Boolean.valueOf(bad))
                .setup(water.getSetup())
                .setupLabel(setup == null ? null : setup.getLabel())
                .pipeDiameter(water.getPipeDiameter())
                .pressureMpa(water.getPressureMpa())
                .flowLps(water.getFlowLps())
                .supplyForm(water.getSupplyForm())
                .supplyFormLabel(supplyForm == null ? null : supplyForm.getLabel())
                .addHeightM(water.getAddHeightM())
                .usable(water.getUsable())
                .usableLabel(usable == null ? null : usable.getLabel())
                .reasonText(water.getReasonText())
                .reasonCode(water.getReasonCode())
                .reasonLabel(reason == null ? null : reason.getLabel())
                .partCode(water.getPartCode())
                .partLabel(part == null ? null : part.getLabel())
                .repair(Boolean.valueOf(water.getIsRepair() != null && water.getIsRepair().intValue() == 1))
                .repairReason(water.getRepairReason())
                .retained(Boolean.valueOf(water.getRetained() != null && water.getRetained().intValue() == 1))
                .drainViewable(water.getDrainViewable())
                .drainUsable(water.getDrainUsable())
                .drainReason(water.getDrainReason())
                .drainNotes(water.getDrainNotes())
                .warmthMeasure(water.getWarmthMeasure() == null
                        ? null : Boolean.valueOf(water.getWarmthMeasure().intValue() == 1))
                .warmthReason(water.getWarmthReason())
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
