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

package eu.occtet.boc.service;

import eu.occtet.boc.model.*;

public abstract class BaseWorkDataProcessor implements IWorkDataProcessor {
    @Override
    public boolean process(AIAnswerWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(AILicenseMatcherWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(FossReportServiceWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(ScannerSendWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(SampleWorkData workData) {
        // default implementation does nothing
        return false;
    }

    @Override
    public boolean process(VulnerabilityServiceWorkData workData) {
        return false;
    }

    @Override
    public boolean process(AIStatusQueryWorkData workData) {
        return false;
    }
    @Override
    public boolean process(AICopyrightFilterWorkData workData) {
        return false;
    }

    @Override
    public boolean process(SpdxWorkData workData){return false;}

    @Override
    public boolean process(DownloadServiceWorkData workData){return false;}

    @Override
    public boolean process(FileIndexingServiceWorkData workData){return false;}

    @Override
    public boolean process(FileSearchServiceWorkData workData){return false;}
}
