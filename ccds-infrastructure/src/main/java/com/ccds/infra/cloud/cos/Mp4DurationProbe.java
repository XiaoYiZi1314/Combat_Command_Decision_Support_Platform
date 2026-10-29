package com.ccds.infra.cloud.cos;

/**
 * 只读解析 MP4 moov/mvhd 的时长，防止客户端伪造视频秒数。
 * 不执行外部程序、不解码视频，也不会根据客户端上传的表单字段相信时长。
 *
 * @author ccds
 * @since 0.1.2
 */
public final class Mp4DurationProbe {

    private static final int BOX_HEADER = 8;

    private static final int EXTENDED_HEADER = 16;

    private static final int MILLIS_PER_SECOND = 1000;

    private Mp4DurationProbe() {
    }

    /**
     * 从 MP4 movie header 读取整段时长。
     *
     * @param data 已通过 MP4 签名校验的文件字节
     * @return 时长毫秒；未知、损坏或无效返回 -1
     */
    public static long durationMillis(byte[] data) {
        if (data == null || data.length < BOX_HEADER) {
            return -1L;
        }
        return findDuration(data, 0, data.length, false);
    }

    private static long findDuration(byte[] data, int from, int end, boolean insideMovie) {
        int offset = from;
        while (offset <= end - BOX_HEADER) {
            long length = uint32(data, offset);
            int header = BOX_HEADER;
            if (length == 1L) {
                if (offset > end - EXTENDED_HEADER) {
                    return -1L;
                }
                length = uint64(data, offset + BOX_HEADER);
                header = EXTENDED_HEADER;
            } else if (length == 0L) {
                length = end - offset;
            }
            if (length < header || length > end - offset) {
                return -1L;
            }
            int body = offset + header;
            int boxEnd = offset + (int) length;
            if (fourcc(data, offset + 4, "moov") && !insideMovie) {
                return findDuration(data, body, boxEnd, true);
            }
            if (fourcc(data, offset + 4, "mvhd") && insideMovie) {
                return movieHeaderDuration(data, body, boxEnd);
            }
            offset = boxEnd;
        }
        return -1L;
    }

    private static long movieHeaderDuration(byte[] data, int from, int end) {
        if (from >= end) {
            return -1L;
        }
        int version = data[from] & 0xFF;
        int scaleOffset = from + (version == 1 ? 20 : 12);
        int durationOffset = scaleOffset + 4;
        int durationBytes = version == 1 ? 8 : 4;
        if ((version != 0 && version != 1) || durationOffset > end - durationBytes) {
            return -1L;
        }
        long scale = uint32(data, scaleOffset);
        long ticks = durationBytes == 8 ? uint64(data, durationOffset) : uint32(data, durationOffset);
        if (scale <= 0L || ticks <= 0L) {
            return -1L;
        }
        double milliseconds = (double) ticks * MILLIS_PER_SECOND / scale;
        return Double.isFinite(milliseconds) && milliseconds <= Long.MAX_VALUE
                ? Math.round(milliseconds) : -1L;
    }

    private static boolean fourcc(byte[] data, int offset, String name) {
        for (int i = 0; i < 4; i++) {
            if (data[offset + i] != (byte) name.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long uint32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static long uint64(byte[] data, int offset) {
        long high = uint32(data, offset);
        long low = uint32(data, offset + 4);
        if (high > Integer.MAX_VALUE) {
            return -1L;
        }
        return (high << 32) | low;
    }
}
