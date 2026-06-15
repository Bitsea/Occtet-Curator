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

public class CycloneDxWorkData extends BaseWorkData{
    @JsonCreator
    public CycloneDxWorkData(@JsonProperty("jsonSpdx")String jsonSpdx,
                        @JsonProperty("bucketName")String bucketName,
                        @JsonProperty("projectId")long projectId,
                        @JsonProperty("useCopyrightAi")boolean useCopyrightAi,
                        @JsonProperty("useLicenseMatcher")boolean useLicenseMatcher,
                             @JsonProperty("withTestLibraries") boolean withTestLibraries) {
        this.jsonSpdx=jsonSpdx;
        this.bucketName=bucketName;
        this.projectId=projectId;
        this.useCopyrightAi = useCopyrightAi;
        this.useLicenseMatcher=useLicenseMatcher;
        this.withTestLibraries= withTestLibraries;
    }

    public CycloneDxWorkData() { }

    private String jsonSpdx;

    private String bucketName;

    private Long projectId;

    private boolean useCopyrightAi;

    private boolean useLicenseMatcher;
    private boolean withTestLibraries;

    private byte[] jsonBytes;

    public String getJsonSpdx() {
        return jsonSpdx;
    }

    public void setJsonSpdx(String jsonSpdx) {
        this.jsonSpdx = jsonSpdx;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public boolean isUseCopyrightAi() {
        return useCopyrightAi;
    }

    public void setUseCopyrightAi(boolean useCopyrightAi) {
        this.useCopyrightAi = useCopyrightAi;
    }

    public boolean isUseLicenseMatcher() {
        return useLicenseMatcher;
    }

    public void setUseLicenseMatcher(boolean useLicenseMatcher) {
        this.useLicenseMatcher = useLicenseMatcher;
    }

    public String getBucketName() {return bucketName;}

    public void setBucketName(String bucketName) {this.bucketName = bucketName;}

    public byte[] getJsonBytes() {return jsonBytes;}

    public void setJsonBytes(byte[] jsonBytes) {this.jsonBytes = jsonBytes;}

    public boolean isWithTestLibraries() {
        return withTestLibraries;
    }

    public void setWithTestLibraries(boolean withTestLibraries) {
        this.withTestLibraries = withTestLibraries;
    }

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
