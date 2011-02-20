/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

/**
 * BluetoothDiscoverableEnabler is a helper to manage the "Discoverable"
 * checkbox. It sets/unsets discoverability and keeps track of how much time
 * until the the discoverability is automatically turned off.
 */
public class BluetoothDiscoverableEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothDiscoverableEnabler";

    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
            "debug.bt.discoverable_time";
    /* package */  static final int DEFAULT_DISCOVERABLE_TIMEOUT = 120;

    /* package */ static final String SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP =
            "discoverable_end_timestamp";

    private final Context mContext;
    private final Handler mUiHandler;
    private final CheckBoxPreference mCheckBoxPreference;
    private final ListPreference mDurationPreference;

    private final LocalBluetoothManager mLocalManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);

                log(mLocalManager.getBluetoothAdapter().getScanMode() ==
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ? "INICIO VISIBLE":"FIN VISIBLE");
                
                if (mode != BluetoothAdapter.ERROR) {                    
                    handleModeChanged(mode);
                }
            }
        }
    };

    private final Runnable mUpdateCountdownSummaryRunnable = new Runnable() {
        public void run() {            
            updateCountdownSummary();            
        }
    };

    public BluetoothDiscoverableEnabler(Context context, CheckBoxPreference checkBoxPreference, ListPreference durationPreference) {
        mContext = context;
        mUiHandler = new Handler();
        mCheckBoxPreference = checkBoxPreference;
        mDurationPreference = durationPreference;

        checkBoxPreference.setPersistent(false);

        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            // Bluetooth not supported
            checkBoxPreference.setEnabled(false);
        }
    }

    public void resume() {
        if (mLocalManager == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mCheckBoxPreference.setOnPreferenceChangeListener(this);
        mCheckBoxPreference.setSummaryOn(null);

        handleModeChanged(mLocalManager.getBluetoothAdapter().getScanMode());
    }

    public void pause() {
        if (mLocalManager == null) {
            return;
        }

        mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
        mCheckBoxPreference.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off BT discoverability
        setEnabled((Boolean) value);

        return true;
    }

    private void setEnabled(final boolean enable) {
        BluetoothAdapter manager = mLocalManager.getBluetoothAdapter();

        if (enable) {
            log("Pulsa en activar");

            int timeout = getDiscoverableTimeout();
            manager.setDiscoverableTimeout(timeout);            

            mCheckBoxPreference.setSummaryOn(null);
            /*if (timeout != 0)                
                mCheckBoxPreference.setSummaryOn(
                        mContext.getResources().getString(R.string.bluetooth_is_discoverable, timeout));*/

            long endTimestamp = System.currentTimeMillis() + timeout * 1000;
            persistDiscoverableEndTimestamp(endTimestamp);

            manager.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                        
            // Retardo para que llegue la señal.
            try { Thread.sleep(50); } catch (InterruptedException e) {e.printStackTrace();}
            handleModeChanged(mLocalManager.getBluetoothAdapter().getScanMode());            
        } else {
            log("Pulsa desactivar");
            mCheckBoxPreference.setSummaryOn(null);
            manager.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }
    }

    private int getDiscoverableTimeout() {
        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        
        try { timeout = Integer.parseInt(mDurationPreference.getValue()); } catch (Exception ex) {}        
        if (timeout < 0) {
            timeout = DEFAULT_DISCOVERABLE_TIMEOUT;
        }

        return timeout;
    }

    private void persistDiscoverableEndTimestamp(long endTimestamp) {
        SharedPreferences.Editor editor = mLocalManager.getSharedPreferences().edit();
        editor.putLong(SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, endTimestamp);
        editor.apply();
    }

    private void handleModeChanged(int mode) {
        if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE || 
                mLocalManager.getBluetoothAdapter().isDiscovering()) {
            log("Llega senyal del BT - Empieza");
            mCheckBoxPreference.setChecked(true);            
            updateCountdownSummary();
        } else {
            log("Llega senyal del BT - Finaliza");
            mCheckBoxPreference.setChecked(false);
        }
    }

    private void updateCountdownSummary() {
        int mode = mLocalManager.getBluetoothAdapter().getScanMode();
        if (mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();
        long endTimestamp = mLocalManager.getSharedPreferences().getLong(
                SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, 0);

        if (currentTimestamp >= endTimestamp) {
            // We're still in discoverable mode, but maybe there isn't a timeout.
            mCheckBoxPreference.setSummaryOn(null);
            return;
        }

        log("En contdown => " + String.valueOf((endTimestamp - currentTimestamp) / 1000));        
        String formattedTimeLeft = String.valueOf((endTimestamp - currentTimestamp) / 1000);

        mCheckBoxPreference.setSummaryOn(
                mContext.getResources().getString(R.string.bluetooth_is_discoverable,
                        formattedTimeLeft));

        synchronized (this) {
            mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
            mUiHandler.postDelayed(mUpdateCountdownSummaryRunnable, 1000);
        }
    }

    private void log(String msg)
    {
        Log.i(TAG, msg);
    }

}
