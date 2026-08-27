package com.ccds.shell.bridge;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;

import com.ccds.shell.BuildConfig;
import com.ccds.shell.MainActivity;
import com.ccds.shell.nfc.NfcSession;
import com.ccds.shell.sensor.HeadingStore;
import com.ccds.shell.sensor.LocationStore;

import org.json.JSONException;
import org.json.JSONObject;

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

    private static final long NFC_WAIT_MS = 15000L;

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
