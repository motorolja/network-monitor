/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.service;

import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBPurge;
import ca.rmen.android.networkmonitor.app.email.ReportEmailer;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.service.datasources.NetMonDataSources;
import ca.rmen.android.networkmonitor.app.service.scheduler.Scheduler;
import ca.rmen.android.networkmonitor.app.sync.SyncPreferences;
import ca.rmen.android.networkmonitor.app.sync.SyncUpload;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * This service periodically retrieves network state information and writes it to the database.
 */
public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();


    private PowerManager mPowerManager;
    private long mLastWakeUp = 0;
    private NetMonDataSources mDataSources;
    private ReportEmailer mReportEmailer;
    private SyncUpload mSyncUpload;
    private Scheduler mScheduler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate Service is enabled: starting monitor loop");

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Show our ongoing notification
        Notification notification = NetMonNotification.createOngoingNotification(this);
        startForeground(NetMonNotification.NOTIFICATION_ID_ONGOING, notification);

        // Prepare our data sources
        mDataSources = new NetMonDataSources();
        mDataSources.onCreate(this);

        mReportEmailer = new ReportEmailer(this);

        mSyncUpload = new SyncUpload(this);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);

        scheduleTests();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        mDataSources.onDestroy();
        NetMonNotification.dismissNotifications(this);
        mScheduler.onDestroy();
        super.onDestroy();
    }

    /**
     * Start scheduling tests, using the scheduler class chosen by the user in the advanced settings.
     */
    private void scheduleTests() {
        Log.v(TAG, "scheduleTests");
        NetMonPreferences prefs = NetMonPreferences.getInstance(this);
        Class<?> schedulerClass = prefs.getSchedulerClass();
        Log.v(TAG, "Will use scheduler " + schedulerClass);
        //noinspection TryWithIdenticalCatches
        try {
            if (mScheduler == null || !mScheduler.getClass().getName().equals(schedulerClass.getName())) {
                Log.v(TAG, "Creating new scheduler " + schedulerClass);
                if (mScheduler != null) mScheduler.onDestroy();
                mScheduler = (Scheduler) schedulerClass.newInstance();
                mScheduler.onCreate(this);
                mScheduler.schedule(mTask, prefs.getUpdateInterval());
            } else {
                Log.v(TAG, "Rescheduling scheduler " + mScheduler);
                int interval = prefs.getUpdateInterval();
                mScheduler.setInterval(interval);
            }

        } catch (InstantiationException e) {
            Log.e(TAG, "setScheduler Could not create scheduler " + schedulerClass + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "setScheduler Could not create scheduler " + schedulerClass + ": " + e.getMessage(), e);
        }
    }

    private final Runnable mTask = new Runnable() {

        @Override
        public void run() {
            Log.v(TAG, "running task");

            // Retrieve the log
            WakeLock wakeLock = null;
            try {
                NetMonPreferences prefs = NetMonPreferences.getInstance(NetMonService.this);
                // Periodically wake up the device to prevent the data connection from being cut.
                long wakeInterval = prefs.getWakeInterval();
                long now = System.currentTimeMillis();
                long timeSinceLastWake = now - mLastWakeUp;
                Log.d(TAG, "wakeInterval = " + wakeInterval + ", lastWakeUp = " + mLastWakeUp + ", timeSinceLastWake = " + timeSinceLastWake);
                if (wakeInterval > 0 && timeSinceLastWake > wakeInterval) {
                    Log.d(TAG, "acquiring lock");
                    //noinspection deprecation
                    wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                    wakeLock.acquire();
                    mLastWakeUp = now;
                }

                // Insert this ContentValues into the DB.
                Log.v(TAG, "Inserting data into DB");
                // Put all the data we want to log, into a ContentValues.
                ContentValues values = new ContentValues();
                values.put(NetMonColumns.TIMESTAMP, System.currentTimeMillis());
                values.putAll(mDataSources.getContentValues());
                getContentResolver().insert(NetMonColumns.CONTENT_URI, values);
                new DBPurge(NetMonService.this, prefs.getDBRecordCount()).execute(null);

                // Send mail
                mReportEmailer.send();

                // Do server sync
                mSyncUpload.Upload();

            } catch (Throwable t) {
                Log.v(TAG, "Error in monitorLoop: " + t.getMessage(), t);
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            }

        }
    };

    private final OnSharedPreferenceChangeListener mSharedPreferenceListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: " + key);
            // Listen for the user disabling the service
            if (NetMonPreferences.PREF_SERVICE_ENABLED.equals(key)) {
                if (!sharedPreferences.getBoolean(key, NetMonPreferences.PREF_SERVICE_ENABLED_DEFAULT)) {
                    Log.v(TAG, "Preference to enable service was turned off");
                    stopSelf();
                }
            }
            // Reschedule our task if the user changed the interval
            else if (NetMonPreferences.PREF_UPDATE_INTERVAL.equals(key) || NetMonPreferences.PREF_SCHEDULER.equals(key)) {
                scheduleTests();
            }
        }
    };

}
