package com.ccds.shell.bridge;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JsBridge 统一 JSON 封装。
 *
 * @author ccds
 * @since 0.1.0
 */
final class BridgeJson {

    private BridgeJson() {
    }

    /**
     * 成功响应。
     *
     * @param data 业务数据，可为 null
     * @return JSON 字符串
     */
    static String ok(JSONObject data) {
        JSONObject root = new JSONObject();
        try {
            root.put("ok", true);
            root.put("data", data == null ? JSONObject.NULL : data);
            root.put("errorCode", "");
        } catch (JSONException ex) {
            return "{\"ok\":false,\"data\":null,\"errorCode\":\"JSON\"}";
        }
        return root.toString();
    }

    /**
     * 失败响应。
     *
     * @param errorCode 错误码
     * @return JSON 字符串
     */
    static String fail(String errorCode) {
        JSONObject root = new JSONObject();
        try {
            root.put("ok", false);
            root.put("data", JSONObject.NULL);
            root.put("errorCode", errorCode == null ? "UNKNOWN" : errorCode);
        } catch (JSONException ex) {
            return "{\"ok\":false,\"data\":null,\"errorCode\":\"JSON\"}";
        }
        return root.toString();
    }
}
