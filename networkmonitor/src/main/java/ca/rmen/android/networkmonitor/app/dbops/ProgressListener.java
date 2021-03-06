/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops;

import ca.rmen.android.networkmonitor.app.dbops.ui.DBOpAsyncTask;

/**
 * Implementations of {@link Task} should notify {@link ProgressListener} of their
 * progress.
 *
 * This is currently used by the {@link DBOpAsyncTask} to execute the long task in
 * the background while showing a progress dialog.
 *
 * If we find we need other long tasks not related to db operations, requiring a
 * progress dialog, we may move this interface to another package.
 */
public interface ProgressListener {
    void onProgress(int progress, int max);
    void onWarning(String message);
}