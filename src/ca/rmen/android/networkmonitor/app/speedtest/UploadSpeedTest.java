/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.speedtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.Log;


/**
 * Uploads a file and calculates the upload speed.
 */
public class UploadSpeedTest {
    private static final String TAG = Constants.TAG + UploadSpeedTest.class.getSimpleName();


    public static SpeedTestResult upload(File file, SpeedTestPreferences.SpeedTestUploadConfig uploadConfig) {
        Log.v(TAG, "upload " + uploadConfig);
        FTPClient ftp = new FTPClient();
        InputStream is = null;
        try {
            ftp.connect(uploadConfig.server, uploadConfig.port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return new SpeedTestResult(0, 0, SpeedTestStatus.FAILURE);
            }
            if (!ftp.login(uploadConfig.user, uploadConfig.password)) {
                ftp.disconnect();
                return new SpeedTestResult(0, 0, SpeedTestStatus.AUTH_FAILURE);
            }
            long before = System.currentTimeMillis();
            is = new FileInputStream(file);
            if (!ftp.storeFile(file.getName(), is)) {
                ftp.disconnect();
                return new SpeedTestResult(0, 0, SpeedTestStatus.FAILURE);
            }

            long after = System.currentTimeMillis();
            ftp.logout();
            ftp.disconnect();
            return new SpeedTestResult((int) file.length(), after - before, SpeedTestStatus.SUCCESS);
        } catch (SocketException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return new SpeedTestResult(0, 0, SpeedTestStatus.FAILURE);
        } catch (IOException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return new SpeedTestResult(0, 0, SpeedTestStatus.FAILURE);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "upload " + e.getMessage(), e);
                }
            }

        }
    }
}