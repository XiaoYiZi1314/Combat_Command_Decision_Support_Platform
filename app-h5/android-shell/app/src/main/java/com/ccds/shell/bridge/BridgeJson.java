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
