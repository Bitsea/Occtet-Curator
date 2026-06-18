/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.occtet.boc.service.IWorkDataProcessor;

public class CycloneDxExportWorkData extends BaseWorkData{

    private Long projectId;
    private String serialNumber;
    private Integer version;
    private Boolean enrichment;
    private String objectStoreKey;

    @JsonCreator
    public CycloneDxExportWorkData(
            @JsonProperty("serialNumber")String serialNumber,
            @JsonProperty("projectId")Long projectId,
            @JsonProperty("version")Integer version,
            @JsonProperty("objectStoreKey")String objectStoreKey,
            @JsonProperty("enrichment")Boolean enrichment
    ){
        this.serialNumber = serialNumber;
        this.projectId = projectId;
        this.version = version;
        this.enrichment = enrichment;
        this.objectStoreKey= objectStoreKey;
    }


    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getEnrichment() {
        return enrichment;
    }

    public void setEnrichment(Boolean enrichment) {
        this.enrichment = enrichment;
    }

    @JsonProperty("objectStoreKey")
    public String getObjectStoreKey() {
        return objectStoreKey;
    }

    public void setObjectStoreKey(String objectStoreKey) {
        this.objectStoreKey = objectStoreKey;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
