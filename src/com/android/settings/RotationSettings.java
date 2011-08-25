/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2011 The CyanogenMod Project
 * Copyright (C) 2011 The SuperTeam Developer Group
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

package com.android.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class RotationSettings extends PreferenceActivity implements OnPreferenceChangeListener {

    private static final String ROTATION_0_PREF = "pref_rotation_0";
    private static final String ROTATION_90_PREF = "pref_rotation_90";
    private static final String ROTATION_180_PREF = "pref_rotation_180";
    private static final String ROTATION_270_PREF = "pref_rotation_270";

    private static final int ROTATION_0_MODE = 8;
    private static final int ROTATION_90_MODE = 1;
    private static final int ROTATION_180_MODE = 2;
    private static final int ROTATION_270_MODE = 4;

    private CheckBoxPreference mRotation0Pref;
    private CheckBoxPreference mRotation90Pref;
    private CheckBoxPreference mRotation180Pref;
    private CheckBoxPreference mRotation270Pref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rotation_settings);

        mRotation0Pref = (CheckBoxPreference) findPreference(ROTATION_0_PREF);
        mRotation90Pref = (CheckBoxPreference) findPreference(ROTATION_90_PREF);
        mRotation180Pref = (CheckBoxPreference) findPreference(ROTATION_180_PREF);
        mRotation270Pref = (CheckBoxPreference) findPreference(ROTATION_270_PREF);
        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_MODE,
                        ROTATION_0_MODE|ROTATION_90_MODE|ROTATION_270_MODE);
        mRotation0Pref.setChecked((mode & ROTATION_0_MODE) != 0);
        mRotation90Pref.setChecked((mode & ROTATION_90_MODE) != 0);
        mRotation180Pref.setChecked((mode & ROTATION_180_MODE) != 0);
        mRotation270Pref.setChecked((mode & ROTATION_270_MODE) != 0);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mRotation0Pref ||
            preference == mRotation90Pref ||
            preference == mRotation180Pref ||
            preference == mRotation270Pref) {
            int mode = 0;
            if (mRotation0Pref.isChecked()) mode |= ROTATION_0_MODE;
            if (mRotation90Pref.isChecked()) mode |= ROTATION_90_MODE;
            if (mRotation180Pref.isChecked()) mode |= ROTATION_180_MODE;
            if (mRotation270Pref.isChecked()) mode |= ROTATION_270_MODE;
            if (mode == 0) {
                mode |= ROTATION_0_MODE;
                mRotation0Pref.setChecked(true);
            }
            Settings.System.putInt(getContentResolver(),
                     Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

}
