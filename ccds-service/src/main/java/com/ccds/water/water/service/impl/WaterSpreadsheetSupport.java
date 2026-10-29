package com.ccds.water.water.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.ccds.water.water.constant.WaterRuleConstant;

/**
 * 现网水源检查记录表的 23 列表头定位与导出模板。
 *
 * <p>列顺序与客户《全市水源明细》一致，导出与导入共用同一套列，导出后再导入条数与状态不变。
 * 这里不再保留旧版 17 列的下标兜底值——兜底下标会把「设置方式」当「类别」读，直接打乱类型统计。</p>
 *
 * @author ccds
 * @since 0.1.0
 */
final class WaterSpreadsheetSupport {

    static final String[] HEADER_SEQ = {"序号"};

    static final String[] HEADER_TYPE = {"类别", "类型"};

    static final String[] HEADER_STATION = {"辖区队站", "队站", "单位"};

    static final String[] HEADER_SETUP = {"设置方式"};

    static final String[] HEADER_ADDRESS = {"地理位置", "位置", "地址"};

    static final String[] HEADER_PIPE_DIAMETER = {"管网直径", "管径"};

    static final String[] HEADER_PRESSURE = {"管网压力", "压力"};

    static final String[] HEADER_FLOW = {"管网流量", "流量"};

    static final String[] HEADER_SUPPLY_FORM = {"给水形式"};

    static final String[] HEADER_ADD_HEIGHT = {"加水高度"};

    static final String[] HEADER_USABLE = {"使用状态", "好用"};

    static final String[] HEADER_REASON = {"无法出水原因", "原因"};

    static final String[] HEADER_REPAIR = {"是否报修"};

    static final String[] HEADER_PHOTO = {"水源照片", "照片"};

    static final String[] HEADER_NOTES = {"备注"};

    static final String[] HEADER_LNG = {"经度", "lng", "longitude"};

    static final String[] HEADER_LAT = {"纬度", "lat", "latitude"};

    static final String[] HEADER_DRAIN_VIEWABLE = {"泄水阀", "泄水阀是否可查看"};

    static final String[] HEADER_DRAIN_USABLE = {"泄水阀好用情况"};

    static final String[] HEADER_DRAIN_REASON = {"无法查看泄水阀原因"};

    static final String[] HEADER_DRAIN_NOTES = {"泄水阀备注"};

    static final String[] HEADER_WARMTH = {"是否有保暖措施", "保暖措施"};

    static final String[] HEADER_WARMTH_REASON = {"无保暖措施原因"};

    /**
     * 标准 23 列。
     */
    static final List<String> EXPORT_HEADERS = List.of(
            "序号", "类别", "辖区队站", "设置方式", "地理位置",
            "管网直径mm", "管网压力Mpa", "管网流量L/S", "给水形式", "加水高度m",
            "使用状态（好用/不好用）", "无法出水原因", "是否报修", "水源照片", "备注",
            "经度", "纬度", "泄水阀", "泄水阀好用情况", "无法查看泄水阀原因",
            "泄水阀备注", "是否有保暖措施", "无保暖措施原因");

    static final List<String> TEMPLATE_EXAMPLE = List.of(
            "1", "消防水鹤", "示例消防站", "地上", "示例道路与示例街交叉口",
            "200", "0.35", "45", "环状", "3.5",
            "好用", "", "否", "", "首行不导入，从第二行开始填写，不要删除首行",
            "123.95", "47.35", "是", "好用", "", "",
            "是", "");

    private WaterSpreadsheetSupport() {
    }

    /**
     * 按表头别名定位列。
     *
     * @param header   表头行
     * @param names    别名
     * @param fallback 未命中时的列下标；-1 表示不兜底
     * @return 列下标
     */
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

    /**
     * 安全取单元格。
     *
     * @param cols  行
     * @param index 列下标
     * @return 文本，越界或空返回空串
     */
    static String cell(List<String> cols, int index) {
        if (cols == null || index < 0 || index >= cols.size() || cols.get(index) == null) {
            return "";
        }
        return cols.get(index).trim();
    }

    /**
     * 是否示例行。
     *
     * @param cols       行
     * @param typeIdx    类别列
     * @param stationIdx 队站列
     * @param addressIdx 地址列
     * @return 示例行为 true
     */
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

    /**
     * 去空白用于表头比对。
     *
     * @param raw 原文
     * @return 去空白文本
     */
    static String compact(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s\\n\\r　]", "");
    }

    /**
     * 生成导入结果行（用于导出）。
     *
     * @return 空列表
     */
    static List<String> emptyRow() {
        return new ArrayList<String>();
    }
}
