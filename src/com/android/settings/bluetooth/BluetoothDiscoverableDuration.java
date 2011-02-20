/* By tolemaC */

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class BluetoothDiscoverableDuration implements Preference.OnPreferenceChangeListener 
{
    private static final int DEFAULT_DISCOVERABLE_TIMEOUT = 120;
 
    private static final String LOG_TAG = "BluetoothDiscoverableDuration";
    
    private Context mContext;
    private ListPreference mPreference;
    
    private Handler mHandler;
    
    // bt_discoverable_duration
    public BluetoothDiscoverableDuration(Context context, ListPreference listPreference) {
        mContext = context;
        mPreference = listPreference;
        
        listPreference.setPersistent(false);
        mPreference.setOnPreferenceChangeListener(this);
        mHandler = new Handler();
    }
    

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        
        log("Valor seleccionado: " + newValue);
        setValue(Integer.parseInt(newValue.toString()));
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
        int duration = getValue();        
                
        if (duration == 0)
            mPreference.setSummary(
                    mContext.getResources().getString(R.string.bluetooth_visibility_duration_summary_always));            
        else
            mPreference.setSummary(
                    mContext.getResources().getString(R.string.bluetooth_visibility_duration_summary,
                            String.valueOf(duration)));
    }
    
    private void setValue(int duration)
    {
        log("Estableciendo el timeout: " + duration);
        if (!Settings.System.putInt(mContext.getContentResolver(), 
                Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT, 
                duration))
            log("No se pudo establecer el tiempo: " + duration);
    }
    
    public int getValue()
    {
        int duration = DEFAULT_DISCOVERABLE_TIMEOUT;
        try
        {
            duration = Settings.System.getInt(mContext.getContentResolver(), 
                    Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT);
        }
        catch (SettingNotFoundException ex)
        {
            log("Setting BLUETOOTH_DISCOVERABILITY_TIMEOUT no encontrada o tiene valor no válido.");
            if (!Settings.System.putInt(mContext.getContentResolver(), 
                    Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT, 
                    duration))
                log("No se pudo establecer el tiempo por defecto.");

        }
        return duration;
    }
    
    public void resume()
    {
        mPreference.setValue(String.valueOf(getValue()));        
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