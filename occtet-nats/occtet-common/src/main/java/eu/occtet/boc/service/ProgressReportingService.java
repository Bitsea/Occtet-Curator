package eu.occtet.boc.service;

import eu.occtet.boc.util.OnProgress;


/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */
public abstract class ProgressReportingService {

    private OnProgress onProgress;

    public void setOnProgress(OnProgress notifyProgress) {
        onProgress = notifyProgress;
    }

    protected void notifyProgress(int percent, String details) {
        if(onProgress != null) {
            onProgress.onProgress(percent,details);
        } else System.out.println("(no progress handler) Progress: " + percent + "% - " + details);
    }


}
