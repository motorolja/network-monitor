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

import java.util.HashSet;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.Log;

@TargetApi(11)
public class SyncPreferences {
    private static final String TAG = Constants.TAG + SyncPreferences.class.getSimpleName();

    public static String PREF_SERVER_SYNCS = "PREF_SERVER_SYNCS";
    static final String PREF_SYNC_ENABLED = "PREF_SYNC_ENABLE";
    static final String PREF_SYNC_FORMATS = "PREF_SYNC_FORMATS";
    static final String PREF_SYNC_SERVER = "PREF_SYNC_SERVER";
    static final String PREF_SYNC_PORT = "PREF_SYNC_PORT";
    static final String PREF_SYNC_USER = "PREF_SYNC_USER";
    static final String PREF_LAST_SYNC_DONE = "PREF_LAST_SYNC_DONE";
    static final String PREF_SYNC_INTERVAL = "PREF_SYNC_INTERVAL";
    static final String PREF_SYNC_PATH = "PREF_SYNC_PATH";
    static final String PREF_SYNC_PROTOCOL = "PREF_SYNC_PROTOCOL";
    private static final String PREF_SYNC_PASSWORD = "PREF_SYNC_PASSWORD";

    private static final String PREF_SYNC_PORT_DEFAULT = "80";
    private static final String PREF_SYNC_INTERVAL_DEFAULT = "0";

    private static SyncPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;
    private static SyncUpload mSyncUpload;


    static class SyncConfig {
        final Set<String> reportFormats;
        final String server;
        final int port;
        final String user;
        final int interval;
        final String password;
        final SyncUpload.UploadType uploadType;
        final String path;

        private SyncConfig(Set<String> reportFormats, String server, int port, String user, String password,
                           int interval, SyncUpload.UploadType uploadType, String path) {
            this.reportFormats = reportFormats;
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.interval = interval;
            this.uploadType = uploadType;
            this.path = path;
        }

        /**
         * @return true if we have enough info to attempt to sync.
         */
        public boolean isValid() {
            return !TextUtils.isEmpty(server) && port > 0;
        }
    }

    /**
     * Main constructor
     */

    static synchronized SyncPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SyncPreferences(context);
        }
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private SyncPreferences(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mSyncUpload = new SyncUpload(context);
    }

    /**
     * Get/set functions for checking/setting the server sync
     */

    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(PREF_SYNC_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        Log.v(TAG, "setEnabled " + enabled);
        mSharedPrefs.edit().putBoolean(PREF_SYNC_ENABLED, enabled).apply();
    }
    /**
     * @return the interval, in milliseconds, between syncs.
     */
    public long getSyncInterval() {
        return getIntPreference(SyncPreferences.PREF_SYNC_INTERVAL, PREF_SYNC_INTERVAL_DEFAULT) * 60 * 60 * 1000;
    }

    /**
     * set the interval, in hours, between syncs.
     */
    public void setSyncInterval(int interval) {
        Editor editor = mSharedPrefs.edit();
        editor.putString(SyncPreferences.PREF_SYNC_INTERVAL, String.valueOf(interval));
        editor.apply();
    }

    /**
     * Sets the last sync to now
     */

    public void setLastSyncDone(long when) {
        Editor editor = mSharedPrefs.edit();
        editor.putLong(SyncPreferences.PREF_LAST_SYNC_DONE, when);
        editor.apply();
    }

    /**
     * @return the last sync time
     */

    public long getLastSyncDone() {
        return mSharedPrefs.getLong(PREF_LAST_SYNC_DONE, 0);
    }

    SyncConfig getSyncConfig() {
        Set<String> reportFormats = mSharedPrefs.getStringSet(PREF_SYNC_FORMATS, new HashSet<String>());
        String server = mSharedPrefs.getString(PREF_SYNC_SERVER, "").trim();
        int port = getIntPreference(PREF_SYNC_PORT, PREF_SYNC_PORT_DEFAULT);
        String user = mSharedPrefs.getString(PREF_SYNC_USER, "").trim();
        String password = mSharedPrefs.getString(PREF_SYNC_PASSWORD, "").trim();
        int interval = getIntPreference(PREF_SYNC_INTERVAL, PREF_SYNC_INTERVAL_DEFAULT);
        String type = mSharedPrefs.getString(PREF_SYNC_PROTOCOL, "").trim();
        SyncUpload.UploadType uploadType = SyncUpload.UploadType.valueOf(type);
        String path = mSharedPrefs.getString(PREF_SYNC_PATH, "").trim();

        return new SyncConfig(reportFormats, server, port, user, password, interval, uploadType, path);
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        return Integer.valueOf(valueStr);
    }

}