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



public class AILicenseMatcherWorkData extends BaseWorkData{


    private String userMessage;
    private String url;
    private String licenseId;
    private String licenseText;
    private Long inventoryItemId;
    private String licenseMatcherResult;

    @JsonCreator
    public AILicenseMatcherWorkData(@JsonProperty("userMessage")String userMessage,
                                    @JsonProperty("url")String url,
                                    @JsonProperty("licenseMatcherResult")String licenseMatcherResult,
                                    @JsonProperty("licenseId")String licenseId,
                                    @JsonProperty("licenseText")String licenseText,
                                    @JsonProperty("inventoryItemId")Long inventoryItemId) {

        this.userMessage= userMessage;
        this.url = url;
        this.licenseMatcherResult = licenseMatcherResult;
        this.licenseId = licenseId;
        this.licenseText = licenseText;
        this.inventoryItemId= inventoryItemId;
    }

    public AILicenseMatcherWorkData(Long inventoryItemId) {

        this.inventoryItemId= inventoryItemId;
    }

    public AILicenseMatcherWorkData(){}

    public String getLicenseMatcherResult() {
        return licenseMatcherResult;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }

    public void setLicenseMatcherResult(String licenseMatcherResult) {
        this.licenseMatcherResult = licenseMatcherResult;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String toString() {
        return "AILicenseMatcherWorkData{userMessage='" + userMessage + "', url='" + url + "'}";
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
