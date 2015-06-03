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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import android.provider.Settings;
import android.text.TextUtils;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

public class FTPSync {
    private static final String TAG = Constants.TAG + FTPSync.class.getSimpleName();

    public static SyncUpload.UploadStatus upload(SyncPreferences.SyncConfig syncConfig, File file, String remoteFileName) {
        FTPClient ftp = new FTPClient();
        InputStream is = null;
        try {
            // Set buffer size of FTP client
            ftp.setBufferSize(1048576);
            // Open a connection to the FTP server
            ftp.connect(syncConfig.server, syncConfig.port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return SyncUpload.UploadStatus.FAILURE;
            }
            // Login to the FTP server
            if (!ftp.login(syncConfig.user, syncConfig.password)) {
                ftp.disconnect();
                return SyncUpload.UploadStatus.AUTH_FAILURE;
            }
            // Try to change directories
            if (!TextUtils.isEmpty(syncConfig.path) && !ftp.changeWorkingDirectory(syncConfig.path)) {
                Log.v(TAG, "Upload: could not change path to " + syncConfig.path);
                return SyncUpload.UploadStatus.INVALID_FILE;
            }

            // set the file type to be read as a binary file
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            // Upload the file
            String uniqueFileName = remoteFileName + System.currentTimeMillis() + '.' + syncConfig.uploadType.toString();
            is = new FileInputStream(file);
            if (!ftp.storeFile(uniqueFileName, is)) {
                ftp.disconnect();
                Log.v(TAG,
                        "Upload: could not store file to " + syncConfig.path + ". Error code: " + ftp.getReplyCode() + ", error string: "
                                + ftp.getReplyString());
                return SyncUpload.UploadStatus.FAILURE;
            }

            ftp.logout();
            ftp.disconnect();
            Log.v(TAG, "Upload complete");
            return SyncUpload.UploadStatus.SUCCESS;
        } catch (SocketException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return SyncUpload.UploadStatus.FAILURE;
        } catch (IOException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return SyncUpload.UploadStatus.FAILURE;
        } finally {
            IoUtil.closeSilently(is);
        }
    }
}
