package com.ccds.assist.assist.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.AttackAdvicePersonDTO;
import com.ccds.assist.assist.dto.SupplyCalcCommand;
import com.ccds.assist.assist.dto.SupplyCalcSourceDTO;
import com.ccds.assist.assist.dto.SupplyCalcVehicleDTO;
import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.enums.AttackStatusEnum;

/**
 * 组装已脱敏的模型提示。禁止电话、住址、精确坐标、完整花名册。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class AssistPromptBuilder {

    private static final String SYSTEM_HAZMAT = "你是消防危化品辅助查询助手。"
            + "只根据名称、俗称、UN、CAS 或标签图片给出危险性与处置要点。"
            + "不要编造未给出的标识。必须声明不替代现场指挥。"
            + "只输出 JSON："
            + "{\"possibleName\":\"\",\"hazardSummary\":\"\",\"advice\":\"\"}。";

    private static final String SYSTEM_ATTACK = "你是内攻安全辅助研判助手。"
            + "只根据未撤出人员的姓名、状态、压力、剩余时间、编组以及天气/现场摘要"
            + "给出轮换、撤离和风险提示。禁止索要或推断电话、住址、证件。"
            + "必须声明不替代现场指挥。只输出 JSON："
            + "{\"rotationAdvice\":\"\",\"withdrawAdvice\":\"\",\"riskHint\":\"\"}。";

    private static final String SYSTEM_SUPPLY = "你是供水辅助研判助手。"
            + "在已有本地计算结果上给出简短增强建议，不得推翻本地需求流量数字。"
            + "必须声明不替代现场指挥。";

    private AssistPromptBuilder() {
    }

    /**
     * 危化品系统提示。
     *
     * @return 系统提示
     */
    public static String hazmatSystem() {
        return SYSTEM_HAZMAT;
    }

    /**
     * 内攻系统提示。
     *
     * @return 系统提示
     */
    public static String attackSystem() {
        return SYSTEM_ATTACK;
    }

    /**
     * 供水系统提示。
     *
     * @return 系统提示
     */
    public static String supplySystem() {
        return SYSTEM_SUPPLY;
    }

    /**
     * 危化品用户提示。
     *
     * @param text 补充文字
     * @return 用户提示
     */
    public static String hazmatUser(String text) {
        StringBuilder builder = new StringBuilder();
        builder.append("请研判可能的危化品及处置要点。");
        if (hasText(text)) {
            builder.append("补充描述：").append(trim(text, AssistRuleConstant.TEXT_MAX_LENGTH));
        }
        return builder.toString();
    }

    /**
     * 内攻用户提示。人员已是白名单字段。
     *
     * @param stationName    站名
     * @param weatherSummary 天气摘要
     * @param sceneDesc      现场描述
     * @param persons        白名单人员
     * @return 用户提示
     */
    public static String attackUser(String stationName, String weatherSummary, String sceneDesc,
                                    List<AttackAdvicePersonDTO> persons) {
        StringBuilder builder = new StringBuilder();
        builder.append("单位：").append(nullToDash(stationName)).append('\n');
        builder.append("天气：")
                .append(nullToDash(trim(weatherSummary, AssistRuleConstant.WEATHER_SUMMARY_MAX_LENGTH)));
        builder.append('\n');
        builder.append("现场：").append(nullToDash(trim(sceneDesc, AssistRuleConstant.SCENE_DESC_MAX_LENGTH)));
        builder.append('\n');
        builder.append("未撤出人员：\n");
        if (persons == null || persons.isEmpty()) {
            builder.append("无\n");
            return builder.toString();
        }
        int limit = Math.min(persons.size(), AssistRuleConstant.ATTACK_PERSON_MAX);
        for (int i = 0; i < limit; i++) {
            AttackAdvicePersonDTO person = persons.get(i);
            builder.append('-')
                    .append(nullToDash(person.getDisplayName()))
                    .append(' ')
                    .append(nullToDash(person.getStatus()))
                    .append(" 压力")
                    .append(formatNumber(person.getPressure()))
                    .append("MPa 剩余")
                    .append(person.getRemainSec() == null ? "-" : person.getRemainSec())
                    .append("秒 编组")
                    .append(nullToDash(person.getGroupName()))
                    .append('\n');
        }
        builder.append("请给出轮换建议、撤离建议和风险提示。");
        return builder.toString();
    }

    /**
     * 供水用户提示。不含火点精确坐标。
     *
     * @param command 本地计算结果
     * @return 用户提示
     */
    public static String supplyUser(SupplyCalcCommand command) {
        StringBuilder builder = new StringBuilder();
        builder.append("火灾类型：").append(nullToDash(command.getFireType())).append('\n');
        builder.append("燃烧面积：").append(formatNumber(command.getArea())).append("m2\n");
        builder.append("经验系数：").append(formatNumber(command.getExperience())).append('\n');
        builder.append("需求流量：").append(formatNumber(command.getRequiredFlow())).append("L/s\n");
        builder.append("车辆泵量：").append(formatNumber(command.getVehicleFlow())).append("L/s\n");
        builder.append("附近水源流量：").append(formatNumber(command.getSourceFlow())).append("L/s\n");
        builder.append("余量：").append(formatNumber(command.getGap())).append("L/s\n");
        if (command.getHoseLoss() != null) {
            builder.append("干线损失：").append(formatNumber(command.getHoseLoss())).append("MPa\n");
        }
        builder.append("车辆：");
        appendVehicles(builder, command.getVehicles());
        builder.append('\n');
        builder.append("水源：");
        appendSources(builder, command.getSources());
        builder.append('\n');
        builder.append("请在本地结论上给出简短增强建议。");
        return builder.toString();
    }

    /**
     * 把内攻卡片收成白名单摘要。跳过已撤出。
     *
     * @param persons 卡片
     * @return 白名单列表，不会为 null
     */
    public static List<AttackAdvicePersonDTO> whitelistPersons(List<AttackPersonDO> persons) {
        List<AttackAdvicePersonDTO> result = new ArrayList<>();
        if (persons == null || persons.isEmpty()) {
            return result;
        }
        for (AttackPersonDO person : persons) {
            if (result.size() >= AssistRuleConstant.ATTACK_PERSON_MAX) {
                break;
            }
            if (person == null) {
                continue;
            }
            AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
            if (status == null || status == AttackStatusEnum.OUT) {
                continue;
            }
            Double pressure = person.getCurrentPressure() == null
                    ? null
                    : person.getCurrentPressure().doubleValue();
            result.add(AttackAdvicePersonDTO.builder()
                    .displayName(trim(person.getDisplayName(), AssistRuleConstant.DISPLAY_NAME_MAX_LENGTH))
                    .status(status.getCode())
                    .pressure(pressure)
                    .remainSec(person.getRemainSec())
                    .groupName(trim(person.getGroupName(), AssistRuleConstant.GROUP_NAME_MAX_LENGTH))
                    .build());
        }
        return result;
    }

    /**
     * 前端可改人员摘要：只保留白名单字段，截断长度。
     *
     * @param persons 前端摘要
     * @return 白名单列表，不会为 null
     */
    public static List<AttackAdvicePersonDTO> sanitizePersons(List<AttackAdvicePersonDTO> persons) {
        List<AttackAdvicePersonDTO> result = new ArrayList<>();
        if (persons == null || persons.isEmpty()) {
            return result;
        }
        int limit = Math.min(persons.size(), AssistRuleConstant.ATTACK_PERSON_MAX);
        for (int i = 0; i < limit; i++) {
            AttackAdvicePersonDTO person = persons.get(i);
            if (person == null) {
                continue;
            }
            result.add(AttackAdvicePersonDTO.builder()
                    .displayName(trim(person.getDisplayName(), AssistRuleConstant.DISPLAY_NAME_MAX_LENGTH))
                    .status(trim(person.getStatus(), AssistRuleConstant.STATUS_CODE_MAX_LENGTH))
                    .pressure(person.getPressure())
                    .remainSec(person.getRemainSec())
                    .groupName(trim(person.getGroupName(), AssistRuleConstant.GROUP_NAME_MAX_LENGTH))
                    .build());
        }
        return result;
    }

    private static void appendVehicles(StringBuilder builder, List<SupplyCalcVehicleDTO> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            builder.append("无");
            return;
        }
        int limit = Math.min(vehicles.size(), AssistRuleConstant.SUPPLY_VEHICLE_MAX);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append('、');
            }
            SupplyCalcVehicleDTO vehicle = vehicles.get(i);
            builder.append(nullToDash(vehicle.getName()))
                    .append(formatNumber(vehicle.getFlow()))
                    .append("L/s");
        }
    }

    private static void appendSources(StringBuilder builder, List<SupplyCalcSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            builder.append("无");
            return;
        }
        int limit = Math.min(sources.size(), AssistRuleConstant.SUPPLY_SOURCE_MAX);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append('、');
            }
            SupplyCalcSourceDTO source = sources.get(i);
            builder.append(nullToDash(source.getName()));
            if (source.getDistanceM() != null) {
                builder.append(source.getDistanceM()).append("米");
            }
            builder.append(formatNumber(source.getEstimateFlow())).append("L/s");
        }
    }

    private static String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static String nullToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
