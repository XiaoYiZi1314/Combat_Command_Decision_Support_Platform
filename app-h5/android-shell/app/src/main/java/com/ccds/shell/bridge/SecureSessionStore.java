package com.ccds.shell.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 使用 Android Keystore 加密会话，仅由宿主的受信主框架消息通道调用。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class SecureSessionStore {

    private static final String KEY_ALIAS = "ccds.session.v1";

    private static final String PREFERENCE_NAME = "ccds_secure_session";

    private static final String SESSION_KEY = "session";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int TAG_LENGTH_BITS = 128;

    private static final int MAX_TOKEN_LENGTH = 16384;

    private final SharedPreferences preferences;

    /**
     * @param context 应用上下文
     */
    public SecureSessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 处理受信页面会话请求。异常只返回错误码，不能向日志或调用方泄露会话原文。
     *
     * @param request 请求 JSON
     * @return 统一桥接响应 JSON
     */
    public synchronized String handle(String request) {
        try {
            JSONObject body = new JSONObject(request);
            JSONObject data;
            switch (body.optString("method")) {
                case "load":
                    data = load();
                    break;
                case "save":
                    save(body.getJSONObject("data"));
                    data = null;
                    break;
                case "clear":
                    if (!preferences.edit().clear().commit()) {
                        return BridgeJson.fail("SESSION_STORAGE_FAILED");
                    }
                    data = null;
                    break;
                default:
                    return BridgeJson.fail("UNSUPPORTED");
            }
            return BridgeJson.ok(data);
        } catch (GeneralSecurityException | IOException | JSONException | IllegalArgumentException ex) {
            return BridgeJson.fail("SESSION_STORAGE_FAILED");
        }
    }

    private JSONObject load() throws GeneralSecurityException, IOException, JSONException {
        String value = preferences.getString(SESSION_KEY, null);
        if (value == null) {
            return null;
        }
        JSONObject envelope = new JSONObject(value);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(TAG_LENGTH_BITS,
                Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)));
        byte[] content = cipher.doFinal(Base64.decode(envelope.getString("content"), Base64.NO_WRAP));
        return new JSONObject(new String(content, StandardCharsets.UTF_8));
    }

    private void save(JSONObject data) throws GeneralSecurityException, IOException, JSONException {
        String accessToken = data.getString("accessToken");
        String refreshToken = data.getString("refreshToken");
        if (accessToken.length() > MAX_TOKEN_LENGTH || refreshToken.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Invalid session length");
        }
        JSONObject tokens = new JSONObject();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        // IV 由 Keystore 加密器生成，不能由 H5 提供或复用。
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] content = cipher.doFinal(tokens.toString().getBytes(StandardCharsets.UTF_8));
        JSONObject envelope = new JSONObject();
        envelope.put("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
        envelope.put("content", Base64.encodeToString(content, Base64.NO_WRAP));
        if (!preferences.edit().putString(SESSION_KEY, envelope.toString()).commit()) {
            throw new IOException("Session persistence failed");
        }
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
