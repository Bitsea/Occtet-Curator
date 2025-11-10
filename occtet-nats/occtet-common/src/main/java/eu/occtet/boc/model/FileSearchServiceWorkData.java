/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

import java.util.UUID;

public class FileSearchServiceWorkData extends BaseWorkData {

    private UUID projectId;
    private String searchText;
    private int maxNumberOfFindings;

    @JsonCreator
    public FileSearchServiceWorkData(@JsonProperty("projectId") UUID projcetId,
                                     @JsonProperty("searchText") String searchText,
                                     @JsonProperty("maxNumberOfFindings") int maxNumberOfFindings) {
        this.projectId = projcetId;
        this.searchText = searchText;
        this.maxNumberOfFindings = maxNumberOfFindings;
    }
    @Override
    public boolean process(IWorkDataProcessor processor) {
        return false;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public int getMaxNumberOfFindings() {
        return maxNumberOfFindings;
    }

    public void setMaxNumberOfFindings(int maxNumberOfFindings) {
        this.maxNumberOfFindings = maxNumberOfFindings;
    }
}
