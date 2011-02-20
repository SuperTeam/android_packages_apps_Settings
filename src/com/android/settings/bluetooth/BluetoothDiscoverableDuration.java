/* By tolemaC */

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

public class BluetoothDiscoverableDuration implements Preference.OnPreferenceChangeListener 
{
    private static final String LOG_TAG = "BluetoothDiscoverableDuration";
    
    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
        "debug.bt.discoverable_time";
    
    private Context mContext;
    private ListPreference mPreference;
    
    private Handler mHandler;
    
    // bt_discoverable_duration
    public BluetoothDiscoverableDuration(Context context, ListPreference listPreference) {
        mContext = context;
        mPreference = listPreference;
        
        mPreference.setOnPreferenceChangeListener(this);
        mHandler = new Handler();
    }
    

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // SystemProperties.set(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, newValue.toString());
        log("Valor seleccionado: " + newValue);
        mHandler.post(new Runnable() {            
            @Override
            public void run() {
                updateSummary();                
            }
        });
        return true;
    }
    
    public void updateSummary()
    {
        int duration = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);        
        
        try { duration = Integer.parseInt(mPreference.getValue()); } catch (Exception ex) {}
                
        log("Discoverable timeout: " + duration + " preference " + duration);
        
        if (duration < 0)
            duration = 95; // Por poner algo raro, nunca se debe dar.
        if (duration == 0)
            mPreference.setSummary(
                    mContext.getResources().getString(R.string.bluetooth_visibility_duration_summary_always));            
        else
            mPreference.setSummary(
                    mContext.getResources().getString(R.string.bluetooth_visibility_duration_summary,
                            String.valueOf(duration)));
    }
    
    public void resume()
    {
        updateSummary();
    }
    
    public void pause()
    {
        
    }
    
    private void log(String msg)
    {
      Log.i(LOG_TAG, msg);
    }

}