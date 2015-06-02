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

import android.content.Context;
import android.telephony.TelephonyManager;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

public class SyncUpload {
    private static final String TAG = Constants.TAG + SyncUpload.class.getSimpleName();

    public enum UploadStatus {
        SUCCESS, INVALID_FILE, FAILURE, AUTH_FAILURE, UNKNOWN, INVALID_UPLOAD_TYPE
    }

    public enum UploadType {
        WebDAV, HTTP, FTP
    }

    private final Context mContext;
    private TelephonyManager mTelephonyManager;

    private static UploadStatus mLastUploadStatus;

    public SyncUpload(Context context) {
        this.mLastUploadStatus = UploadStatus.UNKNOWN;
        this.mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public synchronized void Upload() {
        final SyncPreferences.SyncConfig syncConfig = SyncPreferences.getInstance(mContext).getSyncConfig();
        if (!syncConfig.isValid() || !isSyncTime()) {
            Log.w(TAG, "Cannot perform sync with the current sync settings: " + syncConfig);
            return;
        }
        switch (syncConfig.uploadType) {
            case WebDAV:
                mLastUploadStatus = UploadWebDAV(syncConfig);
                break;
            case HTTP:
                mLastUploadStatus = UploadHTTP(syncConfig);
                break;
            case FTP:
                mLastUploadStatus = UploadFTP(syncConfig);
                break;
            default:
                mLastUploadStatus = UploadStatus.INVALID_UPLOAD_TYPE;
        }
        CheckUploadStatusMessage();
    }

    private boolean isSyncTime() {
        Log.v(TAG, "isSyncTime");
        long syncInterval = SyncPreferences.getInstance(mContext).getSyncInterval();
        if (syncInterval == 0) {
            Log.v(TAG, "isSyncTime: sync is not enabled");
            return false;
        }
        long lastSyncDone = SyncPreferences.getInstance(mContext).getLastSyncDone();
        long now = System.currentTimeMillis();
        Log.v(TAG, "isSyncTime: synced " + (now - lastSyncDone) + " ms ago, vs report duration = " + syncInterval + " ms");
        if (now - lastSyncDone > syncInterval) {
            SyncPreferences.getInstance(mContext).setLastSyncDone(now);
            return true;
        }
        return false;
    }

    private void CheckUploadStatusMessage() {
        // For future error checking
        switch(mLastUploadStatus)
        {
            case UNKNOWN:
                break;
            case INVALID_UPLOAD_TYPE:
                break;
            case INVALID_FILE:
                break;
            case AUTH_FAILURE:
                break;
            case FAILURE:
                break;
            case SUCCESS:
                break;
            default:
                break;
        }
    }

    /**
     *
     * @return parameter telling how the upload went
     */
    private UploadStatus UploadWebDAV (SyncPreferences.SyncConfig syncConfig) {

        return UploadStatus.FAILURE;
    }

    private static UploadStatus UploadHTTP (SyncPreferences.SyncConfig syncConfig) {

        return UploadStatus.FAILURE;
    }

    private UploadStatus UploadFTP (SyncPreferences.SyncConfig syncConfig) {
        File file = createAttachment("db");
        String remoteFileName = mTelephonyManager.getDeviceId();
        return FTPSync.upload(syncConfig, file, remoteFileName);
    }

    /**
     * @param fileType one of the file types the user selected in the preference
     * @return the BodyPart containing the exported file of this type.
     */
    private File createAttachment(String fileType) {
        Log.v(TAG, "createAttachment: fileType = " + fileType);
        // Get the FileExport instance which can export mContext file type.
        final FileExport fileExport;
        switch(fileType) {
            case "csv":
                fileExport = new CSVExport(mContext);
                break;
            case "html":
                fileExport = new HTMLExport(mContext, true);
                break;
            case "excel":
                fileExport = new ExcelExport(mContext);
                break;
            case "kml":
                fileExport = new KMLExport(mContext, NetMonColumns.SOCKET_CONNECTION_TEST);
                break;
            case "db":
            default:
                fileExport = new DBExport(mContext);
                break;
        }
        return fileExport.execute(null);
    }

}
