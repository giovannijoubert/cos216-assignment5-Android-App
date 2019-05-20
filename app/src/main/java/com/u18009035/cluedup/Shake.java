package com.u18009035.cluedup;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Shake implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_GRAVITY = 4.2F;
    private static final int SHAKE_SLOP_TIME_MS = 2000;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private OnShakeListener mListener;
    private long mShakeTimestamp;
    private int mShakeCount;

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    public interface OnShakeListener {
         void onShake(int count);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            //calculate gforce
            float gForceSquared = gX * gX + gY * gY + gZ * gZ;
            float gForce = (float) Math.sqrt(gForceSquared);

            if (gForce > SHAKE_THRESHOLD_GRAVITY) { //if gforce is larger than threshold
                final long now = System.currentTimeMillis();
                //ignore shakes that are to close to each other
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                //reset shake (time threshold elapsed)
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0;
                }

                mShakeTimestamp = now;
                mShakeCount++;

                mListener.onShake(mShakeCount);
            }
        }
    }
}