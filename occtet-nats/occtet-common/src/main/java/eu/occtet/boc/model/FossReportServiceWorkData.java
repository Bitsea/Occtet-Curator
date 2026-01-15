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

    private Long scannerInitializerId;
    private Map<String, Object> rowData;

    @JsonCreator
    public FossReportServiceWorkData(
            @JsonProperty("scannerInitializerId") Long scannerInitializerId,
            @JsonProperty("rowData") Map<String, Object> rowData
    ) {
        this.scannerInitializerId = scannerInitializerId;
        this.rowData = rowData;
    }

    public Long getScannerInitializerId() {
        return scannerInitializerId;
    }

    public void setScannerInitializerId(Long scannerInitializerId) {
        this.scannerInitializerId = scannerInitializerId;
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
}
