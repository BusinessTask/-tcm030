/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eyecool.finger.demo;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.eyecool.finger.demo.utils.ToastUtils;

import android_serialport_api.SerialPortFinder;

public class SerialPortPreferences extends PreferenceActivity {

    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.serial_port_preferences);

        // Devices
        final ListPreference devices = (ListPreference) findPreference("DEVICE");
        String[] entries = mSerialPortFinder.getAllDevices();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();

        if (entries == null || entryValues == null) {
            ToastUtils.showToast(this, "未找到串口设备");
            finish();
        }

        devices.setEntries(entries);
        devices.setEntryValues(entryValues);
        devices.setSummary(devices.getValue());
        devices.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        // Baud rates
        final ListPreference baudrates = (ListPreference) findPreference("BAUDRATE");
        baudrates.setSummary(baudrates.getValue());
        baudrates.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });
    }
}
