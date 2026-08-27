package com.ccds.shell.nfc;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 读取 NFC 标签编号。优先 NDEF 文本，其次 UID。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class NfcTagReader {

    private static final Pattern TAG_NOISE = Pattern.compile("[\\s:：\\-_]");

    private NfcTagReader() {
    }

    /**
     * 从系统 Tag 解析编号。
     *
     * @param tag NFC 标签
     * @return 去空白大写编号，失败返回空串
     */
    public static String readTag(Tag tag) {
        if (tag == null) {
            return "";
        }
        String ndef = readNdef(tag);
        if (!ndef.isEmpty()) {
            return normalize(ndef);
        }
        byte[] id = tag.getId();
        if (id == null || id.length == 0) {
            return "";
        }
        return normalize(bytesToHex(id));
    }

    private static String readNdef(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            return "";
        }
        NdefMessage message = ndef.getCachedNdefMessage();
        if (message == null) {
            try {
                ndef.connect();
                message = ndef.getNdefMessage();
            } catch (Exception ex) {
                return "";
            } finally {
                closeQuiet(ndef);
            }
        }
        if (message == null) {
            return "";
        }
        for (NdefRecord record : message.getRecords()) {
            String text = decodeRecord(record);
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private static String decodeRecord(NdefRecord record) {
        if (record == null) {
            return "";
        }
        short tnf = record.getTnf();
        byte[] type = record.getType();
        byte[] payload = record.getPayload();
        if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type, NdefRecord.RTD_TEXT) && payload != null && payload.length > 1) {
            int langLen = payload[0] & 0x3F;
            boolean utf16 = (payload[0] & 0x80) != 0;
            int start = 1 + langLen;
            if (start >= payload.length) {
                return "";
            }
            Charset cs = utf16 ? Charset.forName("UTF-16") : Charset.forName("UTF-8");
            return new String(payload, start, payload.length - start, cs);
        }
        if (tnf == NdefRecord.TNF_MIME_MEDIA && payload != null && payload.length > 0) {
            return new String(payload, Charset.forName("UTF-8"));
        }
        return "";
    }

    private static String bytesToHex(byte[] id) {
        StringBuilder sb = new StringBuilder(id.length * 2);
        for (byte b : id) {
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }

    private static void closeQuiet(Ndef ndef) {
        try {
            ndef.close();
        } catch (Exception ex) {
            return;
        }
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return TAG_NOISE.matcher(raw).replaceAll("").toUpperCase(Locale.ROOT);
    }
}
