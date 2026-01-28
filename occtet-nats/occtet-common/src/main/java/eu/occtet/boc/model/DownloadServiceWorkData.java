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


public class DownloadServiceWorkData extends BaseWorkData{

    private String downloadURL;
    private Long projectId;
    private Long inventoryItemId;
    private Boolean isMainPackage;

    @JsonCreator
    public DownloadServiceWorkData(@JsonProperty("downloadURL") String downloadURL,
                                   @JsonProperty("projectId") Long projectId,
                                   @JsonProperty("inventoryItemId") Long inventoryItemId,
                                   @JsonProperty("isMainPackage") Boolean isMainPackage) {
        this.downloadURL = downloadURL;
        this.projectId = projectId;
        this.inventoryItemId = inventoryItemId;
        this.isMainPackage = isMainPackage;
    }

    public void setProjectId(Long projectId){this.projectId = projectId;}
    public void setIsMainPackage(Boolean isMainPackage){this.isMainPackage = isMainPackage;}
    public Long getProjectId(){return this.projectId;}
    public Boolean getIsMainPackage(){return this.isMainPackage;}
    public Long getInventoryItemId() {return inventoryItemId;}
    public void setInventoryItemId(Long inventoryItemId) {this.inventoryItemId = inventoryItemId;}
    public String getDownloadURL() {return downloadURL;}
    public void setDownloadURL(String downloadURL) {this.downloadURL = downloadURL;}

    @Override
    public boolean process(IWorkDataProcessor processor) {return processor.process(this);}
}
