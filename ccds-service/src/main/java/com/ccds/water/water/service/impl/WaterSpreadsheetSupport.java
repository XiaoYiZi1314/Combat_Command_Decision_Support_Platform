package com.ccds.water.water.service.impl;

import java.util.List;

import com.ccds.water.water.constant.WaterRuleConstant;

/**
 * 现网水源检查记录表列定位与导出行。
 *
 * @author ccds
 * @since 0.1.0
 */
final class WaterSpreadsheetSupport {

    static final String[] HEADER_TYPE = {"类别", "类型"};

    static final String[] HEADER_STATION = {"辖区队站", "队站", "单位"};

    static final String[] HEADER_SETUP = {"设置方式"};

    static final String[] HEADER_ADDRESS = {"地理位置", "位置", "地址"};

    static final String[] HEADER_USABLE = {"使用状态", "好用"};

    static final String[] HEADER_REPAIR = {"是否报修"};

    static final String[] HEADER_NOTES = {"备注"};

    static final String[] HEADER_LNG = {"经度", "lng", "longitude"};

    static final String[] HEADER_LAT = {"纬度", "lat", "latitude"};

    static final List<String> EXPORT_HEADERS = List.of(
            "类别", "辖区队站", "设置方式", "地理位置", "使用状态（好用/不好用）",
            "是否报修", "备注", "经度", "纬度");

    static final List<String> TEMPLATE_EXAMPLE = List.of(
            "消防水鹤", "示例消防站", "地上", "示例道路与示例街交叉口",
            "好用", "否", "首行不导入，从第二行开始填写，不要删除首行", "123.95", "47.35");

    private WaterSpreadsheetSupport() {
    }

    static int findColumn(List<String> header, String[] names, int fallback) {
        if (header == null) {
            return fallback;
        }
        for (int i = 0; i < header.size(); i++) {
            String cell = compact(header.get(i));
            for (String name : names) {
                String key = compact(name);
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

    static boolean isExampleRow(List<String> cols, int typeIdx, int stationIdx, int addressIdx) {
        String type = cell(cols, typeIdx);
        String station = cell(cols, stationIdx);
        String address = cell(cols, addressIdx);
        if (WaterRuleConstant.TEMPLATE_EXAMPLE_NAME.equals(type)) {
            return true;
        }
        return station.contains(WaterRuleConstant.TEMPLATE_EXAMPLE_NAME)
                || address.contains("示例");
    }

    static String compact(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s\\n\\r　]", "");
    }
}
