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

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
    private static String mUsername;
    private static String mServer;
    private static String mPassword;
    private static int mPort;

    private static UploadStatus mLastUploadStatus;

    SyncUpload (Context context) {
        this.mLastUploadStatus = UploadStatus.UNKNOWN;
        this.mContext = context;
        this.mPort = 80;
    }

    /**
     * Set functions for internal variables
     */
    void setValues (SyncPreferences.SyncConfig syncConfig) {
        mPassword = syncConfig.password;
        mPort = syncConfig.port;
        mUsername = syncConfig.user;
        mServer = syncConfig.server;
    }

    public UploadStatus Upload(UploadType uploadType) {
        switch (uploadType) {
            case WebDAV:
                mLastUploadStatus = UploadWebDAV();
                break;
            case HTTP:
                mLastUploadStatus = UploadHTTP();
                break;
            case FTP:
                mLastUploadStatus = UploadFTP();
                break;
            default:
                mLastUploadStatus = UploadStatus.INVALID_UPLOAD_TYPE;
        }
        return mLastUploadStatus;
    }

    /**
     * Uses sardine which is an open source library for handling WebDav shares
     * Source is found on https://github.com/lookfirst/sardine
     *
     * @return parameter telling how the upload went
     */
    private UploadStatus UploadWebDAV () {
        Sardine sardine;
        if ( mUsername.isEmpty() && mPassword.isEmpty()) {
            sardine = SardineFactory.begin();
        } else {
            sardine = SardineFactory.begin(mUsername, mPassword);
        }
        sardine.enableCompression();
        File exportFile = createAttachment("");
        try {
            byte []data= FileUtils.readFileToByteArray(exportFile);
            sardine.put(mServer,data);
        } catch (IOException e) {
            Log.v(TAG, "Could not copy DB file: " + e.getMessage(), e);
            return UploadStatus.INVALID_FILE;
        }
        return UploadStatus.SUCCESS;
    }

    private static UploadStatus UploadHTTP () {
        return UploadStatus.FAILURE;
    }

    private static UploadStatus UploadFTP () {
        return UploadStatus.FAILURE;
    }

    private static String getPassword () {
        return "";
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
