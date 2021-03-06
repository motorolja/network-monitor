/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Activity which shows an HTML explanation of the speed test limitations
 */
public class SpeedTestAboutActivity extends AppCompatActivity {
    private static final String TAG = Constants.TAG + SpeedTestAboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test_about);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new SpeedTestAboutFragment()).commit();
        }
    }

    public void okClicked(@SuppressWarnings("UnusedParameters") View view) {
        Log.v(TAG, "okClicked");
        finish();
    }

    public void playStoreClicked(@SuppressWarnings("UnusedParameters") View view) {
        Log.v(TAG, "playStoreClicked");
        String playStoreUrl = "https://play.google.com/store/search?c=apps&q=speed test";
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);

    }

}
