package com.ccds.shell.sensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.core.content.ContextCompat;

/**
 * 系统定位缓存。禁止把精确坐标写入日志。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class LocationStore implements LocationListener {

    private final Context context;

    private final LocationManager manager;

    private volatile Location last;

    public LocationStore(Context context) {
        this.context = context.getApplicationContext();
        manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean permitted() {
        int fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    public void start() {
        if (manager == null || !permitted()) {
            return;
        }
        try {
            Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            last = newer(gps, net);
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000L, 8f, this, Looper.getMainLooper());
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 8000L, 20f, this, Looper.getMainLooper());
            }
        } catch (SecurityException ex) {
            last = null;
        }
    }

    public void stop() {
        if (manager == null) {
            return;
        }
        manager.removeUpdates(this);
    }

    public Location last() {
        return last;
    }

    @Override
    public void onLocationChanged(Location location) {
        last = newer(last, location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private static Location newer(Location a, Location b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return b.getTime() >= a.getTime() ? b : a;
    }
}
