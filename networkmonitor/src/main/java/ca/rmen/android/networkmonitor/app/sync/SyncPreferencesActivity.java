/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Rasmus Holm
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

package ca.rmen.android.networkmonitor.app.sync;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.app.prefs.AppCompatPreferenceActivity;
import ca.rmen.android.networkmonitor.util.Log;

@TargetApi(11)
public class SyncPreferencesActivity extends AppCompatPreferenceActivity {
    private static final String TAG = Constants.TAG + SyncPreferencesActivity.class.getSimpleName();

    private SyncPreferences mSyncPreferences;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PreferenceManager.setDefaultValues(this, R.xml.sync_preferences, false);
        addPreferencesFromResource(R.xml.sync_preferences);
        mSyncPreferences = SyncPreferences.getInstance(this);
        updatePreferenceSummary(SyncPreferences.PREF_SYNC_FORMATS, R.string.pref_summary_sync_formats);
        updatePreferenceSummary(SyncPreferences.PREF_SYNC_SERVER, R.string.pref_summary_sync_server);
        updatePreferenceSummary(SyncPreferences.PREF_SYNC_PORT, R.string.pref_summary_sync_port);
        updatePreferenceSummary(SyncPreferences.PREF_SYNC_USER, R.string.pref_summary_sync_user);
        updatePreferenceSummary(SyncPreferences.PREF_SYNC_INTERVAL, R.string.pref_summary_sync_interval);
        updatePreferenceSummary(SyncPreferences.PREF_LAST_SYNC_DONE, R.string.pref_summary_last_sync_done);
        findPreference(SyncPreferences.PREF_SYNC_FORMATS).setOnPreferenceChangeListener(mOnPreferenceChangeListener);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        long syncInterval = mSyncPreferences.getSyncInterval();
        // If the user enabled sync, make sure we have enough info.
        if (mSyncPreferences.isEnabled() && syncInterval > 0) {
            SyncPreferences.SyncConfig syncConfig = mSyncPreferences.getSyncConfig();
            if (!syncConfig.isValid()) {
                mSyncPreferences.setEnabled(false);
                PreferenceDialog.showInfoDialog(this, getString(R.string.missing_sync_settings_info_dialog_title),
                        getString(R.string.missing_sync_settings_info_dialog_message));
            }
        }
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onStop();
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            switch (key) {
                case SyncPreferences.PREF_SYNC_FORMATS:
                    updatePreferenceSummary(SyncPreferences.PREF_SYNC_FORMATS, R.string.pref_summary_sync_formats);
                    break;
                case SyncPreferences.PREF_SYNC_SERVER:
                    updatePreferenceSummary(SyncPreferences.PREF_SYNC_SERVER, R.string.pref_summary_sync_server);
                    break;
                case SyncPreferences.PREF_SYNC_PORT:
                    updatePreferenceSummary(SyncPreferences.PREF_SYNC_PORT, R.string.pref_summary_sync_port);
                    break;
                case SyncPreferences.PREF_SYNC_USER:
                    updatePreferenceSummary(SyncPreferences.PREF_SYNC_USER, R.string.pref_summary_sync_user);
                    break;
                case SyncPreferences.PREF_SYNC_INTERVAL:
                    updatePreferenceSummary(SyncPreferences.PREF_SYNC_INTERVAL, R.string.pref_summary_sync_interval);
                    break;
                case SyncPreferences.PREF_LAST_SYNC_DONE:
                    updatePreferenceSummary(SyncPreferences.PREF_LAST_SYNC_DONE, R.string.pref_summary_last_sync_done);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * @return a String containing the user-friendly names of the values selected by the user.
     */
    private String getSummary(MultiSelectListPreference preference, Set<String> values) {
        List<CharSequence> result = new ArrayList<>();
        CharSequence[] entries = preference.getEntries();
        for (String value : values) {
            int index = preference.findIndexOfValue(value);
            result.add(entries[index]);
        }
        return TextUtils.join(", ", result);
    }

    private void updatePreferenceSummary(CharSequence key, int summaryResId) {
        @SuppressWarnings("deprecation")
        Preference pref = getPreferenceManager().findPreference(key);
        CharSequence value;
        if (key.equals(SyncPreferences.PREF_LAST_SYNC_DONE)) {
            long lastSync = SyncPreferences.getInstance(this).getLastSyncDone();
            if (lastSync > 0) {
                value = DateUtils.formatDateTime(this, lastSync, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR);
            } else {
                value = getString(R.string.pref_value_sync_interval_never);
            }
        } else if (pref instanceof EditTextPreference) {
            value = ((EditTextPreference) pref).getText();
        } else if (pref instanceof MultiSelectListPreference) {
            value = getSummary((MultiSelectListPreference) pref, ((MultiSelectListPreference) pref).getValues());
        } else {
            return;
        }
        String summary = getString(summaryResId, value);
        pref.setSummary(summary);
    }

    /**
     * A fix to guarantee that the multi list is updated correctly
     */
    private final Preference.OnPreferenceChangeListener mOnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.v(TAG, "onPreferenceChange: preference = " + preference + ", newValue = " + newValue);
            if (SyncPreferences.PREF_SYNC_FORMATS.equals(preference.getKey())) {
                @SuppressWarnings("unchecked")
                String valueStr = getSummary((MultiSelectListPreference) preference, (Set<String>) newValue);
                String summary = getString(R.string.pref_summary_sync_formats, valueStr);
                preference.setSummary(summary);
            }
            return true;
        }
    };

}
