package com.ccds.roster.roster.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.ccds.roster.roster.vo.BattleGroupVO;
import com.ccds.roster.roster.vo.GroupMemberVO;
import com.ccds.roster.roster.vo.ProfileVO;

/**
 * 现网花名册表格列定位与导出行。
 *
 * @author ccds
 * @since 0.1.0
 */
final class RosterSpreadsheetSupport {

    static final String[] HEADER_NAME = {"姓名"};

    static final String[] HEADER_TITLE = {"职务/角色", "职务", "角色"};

    static final String[] HEADER_RANK = {"消防救援衔", "救援衔"};

    static final String[] HEADER_PHONE = {"电话号", "电话", "手机号", "联系电话"};

    static final String[] HEADER_CYL = {"气瓶规格", "气瓶"};

    static final String[] HEADER_NFC = {"NFC标号", "NFC编号", "NFC", "卡号", "卡扣编号"};

    static final String[] HEADER_GROUP = {"所属战斗编组", "战斗编组", "所属战斗小组", "战斗小组", "小组"};

    static final String[] HEADER_HEIGHT = {"身高cm", "身高"};

    static final String[] HEADER_WEIGHT = {"体重kg", "体重"};

    static final String[] HEADER_AGE = {"年龄"};

    static final String[] HEADER_MORNING = {"早检查空呼压力", "早检压力", "空呼压力"};

    static final List<String> EXPORT_HEADERS = List.of(
            "姓名", "职务/角色", "消防救援衔", "电话号", "气瓶规格(6.8/9)", "NFC标号",
            "所属战斗编组", "身高cm", "体重kg", "年龄", "早检查空呼压力(MPa)");

    static final List<String> TEMPLATE_EXAMPLE = List.of(
            "张三", "消防员", "二级消防士", "13800000000", "6.8", "04A1B2C3D4",
            "灭火1组", "175", "70", "28", "25.0");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private RosterSpreadsheetSupport() {
    }

    static int findColumn(List<String> header, String[] names, int fallback) {
        if (header == null) {
            return fallback;
        }
        for (int i = 0; i < header.size(); i++) {
            String cell = compact(header.get(i)).toLowerCase(Locale.ROOT);
            for (String name : names) {
                String key = compact(name).toLowerCase(Locale.ROOT);
                if (cell.equals(key) || cell.contains(key)) {
                    return i;
                }
            }
        }
        return fallback;
    }

    static String cell(List<String> cols, int index) {
        if (cols == null || index < 0 || index >= cols.size() || cols.get(index) == null) {
            return "";
        }
        return cols.get(index).trim();
    }

    static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static List<String> toExportRow(ProfileVO profile, String groupName) {
        List<String> row = new ArrayList<String>();
        row.add(nullToEmpty(profile.getName()));
        row.add(nullToEmpty(profile.getTitle()));
        row.add(nullToEmpty(profile.getRankName()));
        row.add(nullToEmpty(profile.getPhone()));
        row.add(nullToEmpty(profile.getCylType()));
        row.add(nullToEmpty(profile.getNfcTag()));
        row.add(nullToEmpty(groupName));
        row.add(profile.getHeightCm() == null ? "" : String.valueOf(profile.getHeightCm()));
        row.add(profile.getWeightKg() == null ? "" : String.valueOf(profile.getWeightKg()));
        row.add(profile.getAge() == null ? "" : String.valueOf(profile.getAge()));
        row.add(profile.getMorningPressure() == null ? "" : profile.getMorningPressure().toPlainString());
        return row;
    }

    static Map<Long, String> firstGroupNameByProfile(List<BattleGroupVO> groups) {
        Map<Long, String> map = new LinkedHashMap<Long, String>();
        if (groups == null) {
            return map;
        }
        for (BattleGroupVO group : groups) {
            if (group.getMembers() == null) {
                continue;
            }
            for (GroupMemberVO member : group.getMembers()) {
                map.putIfAbsent(member.getProfileId(), group.getName());
            }
        }
        return map;
    }

    private static String compact(String raw) {
        return raw == null ? "" : WHITESPACE.matcher(raw).replaceAll("");
    }

    private static String nullToEmpty(String raw) {
        return raw == null ? "" : raw;
    }
}
