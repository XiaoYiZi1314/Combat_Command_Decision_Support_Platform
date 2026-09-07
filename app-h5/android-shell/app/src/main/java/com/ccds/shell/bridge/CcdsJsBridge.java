package com.ccds.shell.bridge;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;

import com.ccds.shell.BuildConfig;
import com.ccds.shell.MainActivity;
import com.ccds.shell.nfc.NfcSession;
import com.ccds.shell.sensor.HeadingStore;
import com.ccds.shell.sensor.LocationStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * H5 JsBridge。方法名与 app-h5/src/bridge 对齐。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class CcdsJsBridge {

    public static final String NAME = "CcdsNativeBridge";

    private static final String UNSUPPORTED = "UNSUPPORTED";

    private static final String NO_NFC = "NO_NFC";

    private static final String NFC_TIMEOUT = "NFC_TIMEOUT";

    private static final String NO_LOCATION = "NO_LOCATION";

    private static final String NO_HEADING = "NO_HEADING";

    private static final String INVALID_URL = "INVALID_URL";

    private static final String NO_FILE = "NO_FILE";

    private static final String SAVE_FAILED = "SAVE_FAILED";

    private static final long NFC_WAIT_MS = 15000L;

    /** pickFile 的回调请求码，仅用于与其它 Activity 结果区分 */
    private static final int REQ_PICK_FILE = 71;

    private final MainActivity activity;

    private final NfcSession nfcSession;

    private final HeadingStore headingStore;

    private final LocationStore locationStore;

    private TextToSpeech tts;

    /**
     * 绑定宿主 Activity 与设备能力。
     *
     * @param activity      宿主
     * @param nfcSession    NFC 会话
     * @param headingStore  罗盘
     * @param locationStore 定位
     */
    public CcdsJsBridge(MainActivity activity, NfcSession nfcSession, HeadingStore headingStore,
                        LocationStore locationStore) {
        this.activity = activity;
        this.nfcSession = nfcSession;
        this.headingStore = headingStore;
        this.locationStore = locationStore;
    }

    /**
     * 同步能力清单，供 H5 探测。
     *
     * @return 能力 JSON
     */
    @JavascriptInterface
    public String capabilities() {
        return capabilityJson().toString();
    }

    /**
     * 按方法名调用设备能力。
     *
     * @param method      方法名
     * @param payloadJson JSON 入参
     * @return 统一 `{ok,data,errorCode}` JSON
     */
    @JavascriptInterface
    public String invoke(String method, String payloadJson) {
        String name = method == null ? "" : method;
        switch (name) {
            case "getCapabilities":
                return BridgeJson.ok(capabilityJson());
            case "nfcRead":
                return nfcRead();
            case "heading":
                return heading();
            case "locate":
                return locate();
            case "speak":
                return speak(payloadJson);
            case "openUrl":
                return openUrl(payloadJson);
            case "getVersion":
                return version();
            case "getDeviceCompatInfo":
                return deviceCompat();
            case "saveFile":
                return saveFile(payloadJson);
            case "pickFile":
                return pickFile(payloadJson);
            default:
                return BridgeJson.fail(UNSUPPORTED);
        }
    }

    private JSONObject capabilityJson() {
        JSONObject data = new JSONObject();
        put(data, "nfc", nfcSession.hardwarePresent());
        put(data, "locate", true);
        put(data, "heading", headingStore.available());
        put(data, "speak", true);
        put(data, "openUrl", true);
        put(data, "source", "android");
        return data;
    }

    private String nfcRead() {
        if (!nfcSession.hardwarePresent()) {
            return BridgeJson.fail(NO_NFC);
        }
        String tag = nfcSession.awaitTag(NFC_WAIT_MS);
        if (tag == null || tag.isEmpty()) {
            return BridgeJson.fail(NFC_TIMEOUT);
        }
        JSONObject data = new JSONObject();
        put(data, "tag", tag);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String heading() {
        Float deg = headingStore.degrees();
        if (deg == null) {
            return BridgeJson.fail(NO_HEADING);
        }
        JSONObject data = new JSONObject();
        put(data, "degrees", deg.doubleValue());
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String locate() {
        if (!locationStore.permitted()) {
            activity.requestLocationPermission();
            return BridgeJson.fail(NO_LOCATION);
        }
        Location loc = locationStore.last();
        if (loc == null) {
            return BridgeJson.fail(NO_LOCATION);
        }
        JSONObject data = new JSONObject();
        put(data, "lng", loc.getLongitude());
        put(data, "lat", loc.getLatitude());
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String speak(String payloadJson) {
        String text = readString(payloadJson, "text");
        if (text == null || text.isEmpty()) {
            JSONObject data = new JSONObject();
            put(data, "spoken", false);
            put(data, "source", "android");
            return BridgeJson.ok(data);
        }
        activity.runOnUiThread(() -> speakOnUi(text));
        JSONObject data = new JSONObject();
        put(data, "spoken", true);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private void speakOnUi(String text) {
        if (tts == null) {
            tts = new TextToSpeech(activity.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage(Locale.CHINA);
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ccds-speak");
                }
            });
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ccds-speak");
    }

    private String openUrl(String payloadJson) {
        String url = readString(payloadJson, "url");
        if (url == null || url.isEmpty()) {
            return BridgeJson.fail(INVALID_URL);
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme) && !"geo".equals(scheme) && !"baidumap".equals(scheme)) {
            return BridgeJson.fail(INVALID_URL);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            return BridgeJson.fail(UNSUPPORTED);
        }
        JSONObject data = new JSONObject();
        put(data, "opened", true);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String version() {
        JSONObject data = new JSONObject();
        put(data, "versionName", BuildConfig.VERSION_NAME);
        put(data, "versionCode", BuildConfig.VERSION_CODE);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String deviceCompat() {
        String brand = Build.BRAND == null ? "" : Build.BRAND;
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        String display = Build.DISPLAY == null ? "" : Build.DISPLAY;
        String text = (brand + " " + manufacturer + " " + model + " " + display).toLowerCase(Locale.ROOT);
        boolean huawei = text.contains("huawei") || text.contains("honor") || text.contains("harmony") || text.contains("hmos");
        JSONObject data = new JSONObject();
        put(data, "huaweiHarmony", huawei);
        put(data, "brand", brand);
        put(data, "manufacturer", manufacturer);
        put(data, "model", model);
        put(data, "display", display);
        put(data, "sdk", Build.VERSION.SDK_INT);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    /**
     * 保存 H5 生成的文件（base64 内容）到系统公共下载目录。
     * Android 10+ 通过 MediaStore 插入 Downloads；老版本直接写外部下载目录并广播媒体扫描。
     * JavascriptInterface 运行在专用桥线程，可直接同步写盘。
     *
     * @param payloadJson 含 fileName 与 base64（文件内容）
     * @return 统一 `{ok,data,errorCode}` JSON，data.path 为保存路径描述
     */
    private String saveFile(String payloadJson) {
        String fileName = readString(payloadJson, "fileName");
        String base64 = readString(payloadJson, "base64");
        if (fileName == null || fileName.isEmpty() || base64 == null || base64.isEmpty()) {
            return BridgeJson.fail("INVALID_PARAM");
        }
        byte[] body;
        try {
            body = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP);
        } catch (IllegalArgumentException ex) {
            return BridgeJson.fail("INVALID_PARAM");
        }
        if (body.length == 0) {
            return BridgeJson.fail("INVALID_PARAM");
        }
        String path = saveToDownloads(sanitizeFileName(fileName), body);
        if (path == null) {
            return BridgeJson.fail(SAVE_FAILED);
        }
        JSONObject data = new JSONObject();
        put(data, "path", path);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    /**
     * 打开系统文件选择器（ACTION_GET_CONTENT）。因需跨 Activity 回调，
     * 选择结果由宿主 Activity 在 onActivityResult 中回推给 H5（__ccdsFilePicked 事件）。
     *
     * @param payloadJson 含 mime（可为空，默认任意类型）
     * @return 统一 `{ok,data,errorCode}` JSON，accepted 表示选择器已拉起
     */
    private String pickFile(String payloadJson) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String mime = readString(payloadJson, "mime");
        if (mime == null || mime.isEmpty()) {
            mime = "*/*";
        }
        intent.setType(mime);
        try {
            activity.startActivityForResult(intent, REQ_PICK_FILE);
        } catch (ActivityNotFoundException ex) {
            return BridgeJson.fail(UNSUPPORTED);
        }
        JSONObject data = new JSONObject();
        put(data, "accepted", true);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    /**
     * 供宿主 Activity 分发文件选择结果：读出文件内容并回推给 H5。
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回的 Intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_PICK_FILE) {
            return;
        }
        String resultJson;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            resultJson = BridgeJson.fail(NO_FILE);
        } else {
            resultJson = readPickedFile(data.getData());
        }
        final String payload = resultJson;
        String js = "window.__ccdsFilePicked&&window.__ccdsFilePicked(" + JSONObject.quote(payload) + ")";
        activity.runOnUiThread(() -> activity.evaluateJs(js, null));
    }

    private String readPickedFile(Uri uri) {
        String fileName = queryDisplayName(uri);
        byte[] bytes = readAllBytes(uri);
        if (bytes == null) {
            return BridgeJson.fail(SAVE_FAILED);
        }
        JSONObject data = new JSONObject();
        put(data, "fileName", fileName);
        put(data, "base64", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP));
        put(data, "size", bytes.length);
        put(data, "source", "android");
        return BridgeJson.ok(data);
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = cursor.getString(idx);
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ex) {
            // 查询失败时退回 uri 末段
        }
        String last = uri.getLastPathSegment();
        return last == null ? "file" : last;
    }

    private byte[] readAllBytes(Uri uri) {
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } catch (IOException | SecurityException ex) {
            return null;
        }
    }

    /**
     * 写入公共下载目录，返回保存路径描述；失败返回 null。
     *
     * @param fileName 目标文件名（已消毒）
     * @param body     文件内容
     * @return 成功返回路径描述，失败返回 null
     */
    private String saveToDownloads(String fileName, byte[] body) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, guessMime(fileName));
                Uri target = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (target == null) {
                    return null;
                }
                try (OutputStream out = activity.getContentResolver().openOutputStream(target)) {
                    if (out == null) {
                        return null;
                    }
                    out.write(body);
                }
                return "Downloads/" + fileName;
            }
            java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            java.io.File target = new java.io.File(dir, fileName);
            try (OutputStream out = new java.io.FileOutputStream(target)) {
                out.write(body);
            }
            android.content.Intent scan = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(target));
            activity.sendBroadcast(scan);
            return target.getAbsolutePath();
        } catch (IOException | SecurityException ex) {
            return null;
        }
    }

    /**
     * 清理文件名：去掉路径分隔符与 Windows/Android 非法字符。
     *
     * @param name 原始文件名
     * @return 安全文件名
     */
    private static String sanitizeFileName(String name) {
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.isEmpty() ? "file" : cleaned;
    }

    private static String guessMime(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls":
                return "application/vnd.ms-excel";
            case "csv":
                return "text/csv";
            case "html":
            case "htm":
                return "text/html";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 释放 TTS。
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private static String readString(String payloadJson, String key) {
        if (payloadJson == null || payloadJson.isEmpty()) {
            return "";
        }
        try {
            JSONObject obj = new JSONObject(payloadJson);
            return obj.optString(key, "");
        } catch (JSONException ex) {
            return "";
        }
    }

    private static void put(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException ex) {
            return;
        }
    }
}
