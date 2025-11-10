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

public interface IWorkDataProcessor {

    boolean process(AIAnswerWorkData workData);
    boolean process(AILicenseMatcherWorkData workData);
    boolean process(FossReportServiceWorkData workData);
    boolean process(ScannerSendWorkData workData);
    boolean process(SampleWorkData workData);
    boolean process(AIStatusQueryWorkData workData);
    boolean process(VulnerabilityServiceWorkData workData);
    boolean process(SpdxWorkData workData);
    boolean process(AICopyrightFilterWorkData workData);
    boolean process(DownloadServiceWorkData workData);
    boolean process(FileIndexingServiceWorkData workData);
    boolean process(FileSearchServiceWorkData workData);

}
