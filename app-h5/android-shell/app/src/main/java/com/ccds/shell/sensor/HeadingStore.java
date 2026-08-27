package com.ccds.shell.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 罗盘航向缓存。无传感器时返回空。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class HeadingStore implements SensorEventListener {

    private final SensorManager manager;

    private final Sensor rotation;

    private final float[] rotationMatrix = new float[9];

    private final float[] orientation = new float[3];

    private volatile Float degrees;

    /**
     * @param context 应用上下文
     */
    public HeadingStore(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotation = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    /**
     * @return 是否有旋转矢量传感器
     */
    public boolean available() {
        return rotation != null;
    }

    /**
     * 订阅航向。
     */
    public void start() {
        if (manager == null || rotation == null) {
            return;
        }
        manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * 停止订阅。
     */
    public void stop() {
        if (manager == null) {
            return;
        }
        manager.unregisterListener(this);
    }

    /**
     * @return 航向角度，无读数时为空
     */
    public Float degrees() {
        return degrees;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null) {
            return;
        }
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);
        float azimuth = (float) Math.toDegrees(orientation[0]);
        float normalized = (azimuth + 360f) % 360f;
        degrees = normalized;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
