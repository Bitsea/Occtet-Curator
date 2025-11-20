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

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.occtet.boc.service.IWorkDataProcessor;

/**
 * base class for work data which is sent to microservices inside a WorkTask.
 * For your own microservice or type of work data, add a subclass and implement the process method with just return processor.process(this);
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AIAnswerWorkData.class, name = "ai_answer"),
        @JsonSubTypes.Type(value = AILicenseMatcherWorkData.class, name = "licenseMatcher_task"),
        @JsonSubTypes.Type(value = AIStatusQueryWorkData.class, name = "ai_status"),
        @JsonSubTypes.Type(value = ScannerSendWorkData.class, name = "scannerdata_send"),
        @JsonSubTypes.Type(value = AICopyrightFilterWorkData.class, name = "copyrightFilter_task"),
        @JsonSubTypes.Type(value = FossReportServiceWorkData.class, name = "fossreport_task"),
        @JsonSubTypes.Type(value = SpdxWorkData.class, name = "spdx_task"),
        @JsonSubTypes.Type(value = SampleWorkData.class, name = "sample"),
        @JsonSubTypes.Type(value = VulnerabilityServiceWorkData.class, name = "vulnerability_task"),
        @JsonSubTypes.Type(value = DownloadServiceWorkData.class, name = "download_task"),
        @JsonSubTypes.Type(value = FileIndexingServiceWorkData.class, name = "file_indexing_task"),
})
public abstract class BaseWorkData {
    public abstract boolean process(IWorkDataProcessor processor);

}
