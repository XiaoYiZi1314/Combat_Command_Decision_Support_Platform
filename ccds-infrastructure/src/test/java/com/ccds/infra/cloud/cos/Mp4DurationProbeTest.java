package com.ccds.infra.cloud.cos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 无额外测试依赖的 MP4 时长安全回归测试；可直接用 JDK javac/java 执行。
 *
 * @author ccds
 * @since 0.1.2
 */
public final class Mp4DurationProbeTest {

    private Mp4DurationProbeTest() {
    }

    public static void main(String[] args) {
        check(7500L, Mp4DurationProbe.durationMillis(mp4(0, 7500L)), "v0 7.5 秒");
        check(10000L, Mp4DurationProbe.durationMillis(mp4(1, 10000L)), "v1 10 秒");
        check(12000L, Mp4DurationProbe.durationMillis(mp4(0, 12000L)), "超长检测");
        check(-1L, Mp4DurationProbe.durationMillis(new byte[]{0, 0, 0, 2}), "不完整 box");
        byte[] invalid = mp4(0, 5000L);
        invalid[3] = 1;
        check(-1L, Mp4DurationProbe.durationMillis(invalid), "错误 box 长度");
    }

    private static byte[] mp4(int version, long millis) {
        int mvhdPayloadLength = version == 1 ? 32 : 20;
        int mvhdSize = 8 + mvhdPayloadLength;
        ByteBuffer data = ByteBuffer.allocate(8 + mvhdSize).order(ByteOrder.BIG_ENDIAN);
        data.putInt(8 + mvhdSize).put(new byte[]{'m', 'o', 'o', 'v'});
        data.putInt(mvhdSize).put(new byte[]{'m', 'v', 'h', 'd'});
        data.put((byte) version).put(new byte[]{0, 0, 0});
        if (version == 1) {
            data.putLong(0).putLong(0).putInt(1000).putLong(millis);
        } else {
            data.putInt(0).putInt(0).putInt(1000).putInt((int) millis);
        }
        return data.array();
    }

    private static void check(long expected, long actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
