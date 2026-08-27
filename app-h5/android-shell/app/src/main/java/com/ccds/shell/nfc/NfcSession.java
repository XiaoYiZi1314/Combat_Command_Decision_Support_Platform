package com.ccds.shell.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import com.ccds.shell.BuildConfig;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * NFC 前台调度与一次读取会话。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class NfcSession {

    private final Activity activity;

    private final NfcAdapter adapter;

    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

    /**
     * @param activity 宿主 Activity
     */
    public NfcSession(Activity activity) {
        this.activity = activity;
        adapter = NfcAdapter.getDefaultAdapter(activity);
    }

    /**
     * @return 是否有 NFC 硬件，或 debug 模拟开启
     */
    public boolean hardwarePresent() {
        return adapter != null || BuildConfig.DEBUG_NFC;
    }

    /**
     * @return 适配器已开启
     */
    public boolean enabled() {
        return adapter != null && adapter.isEnabled();
    }

    /**
     * 进入前台调度。
     */
    public void enableForeground() {
        if (adapter == null) {
            return;
        }
        Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(activity, 0, intent, flags);
        adapter.enableForegroundDispatch(activity, pending, null, null);
    }

    /**
     * 退出前台调度。
     */
    public void disableForeground() {
        if (adapter == null) {
            return;
        }
        adapter.disableForegroundDispatch(activity);
    }

    /**
     * 从 Intent 解析标签并入队。
     *
     * @param intent 系统 NFC Intent
     */
    public void onNewIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String value = NfcTagReader.readTag(tag);
        if (!value.isEmpty()) {
            offer(value);
        }
    }

    /**
     * 阻塞等待一次读卡。
     *
     * @param timeoutMs 超时毫秒
     * @return 编号，超时返回空串
     */
    public String awaitTag(long timeoutMs) {
        queue.clear();
        if (BuildConfig.DEBUG_NFC && adapter == null) {
            return "DEBUG-NFC-0001";
        }
        try {
            String tag = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return tag == null ? "" : tag;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    private void offer(String tag) {
        queue.clear();
        queue.offer(tag);
    }
}
