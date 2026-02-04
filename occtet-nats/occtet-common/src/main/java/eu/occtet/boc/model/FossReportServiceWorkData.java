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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.Map;


public class FossReportServiceWorkData extends BaseWorkData{

    private Long projectId;
    private Map<String, Object> rowData;
    private boolean useLicenseMatcher;
    private boolean useCopyrightFilter;

    @JsonCreator
    public FossReportServiceWorkData(
            @JsonProperty("projectId") Long projectId,
            @JsonProperty("rowData") Map<String, Object> rowData
    ) {
        this.projectId = projectId;
        this.rowData = rowData;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Map<String, Object> getRowData() {
        return rowData;
    }

    public void setRowData(Map<String, Object> rowData) {
        this.rowData = rowData;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }

    public boolean isUseLicenseMatcher() {
        return useLicenseMatcher;
    }

    public void setUseLicenseMatcher(boolean useLicenseMatcher) {
        this.useLicenseMatcher = useLicenseMatcher;
    }

    public boolean isUseCopyrightFilter() {
        return useCopyrightFilter;
    }

    public void setUseCopyrightFilter(boolean useCopyrightFilter) {
        this.useCopyrightFilter = useCopyrightFilter;
    }
}
