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

    public HeadingStore(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotation = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public boolean available() {
        return rotation != null;
    }

    public void start() {
        if (manager == null || rotation == null) {
            return;
        }
        manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        if (manager == null) {
            return;
        }
        manager.unregisterListener(this);
    }

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
