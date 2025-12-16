/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class SpdxExportWorkData extends BaseWorkData{

    @JsonCreator
    public SpdxExportWorkData(
            @JsonProperty("spdxDocumentId")String spdxDocumentId,
            @JsonProperty("projectId")String projectId
    ){
        this.spdxDocumentId = spdxDocumentId;
        this.projectId = projectId;
    }

    public SpdxExportWorkData(){}

    private String spdxDocumentId;
    private String projectId;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSpdxDocumentId() {
        return spdxDocumentId;
    }

    public void setSpdxDocumentId(String spdxDocumentId) {
        this.spdxDocumentId = spdxDocumentId;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
