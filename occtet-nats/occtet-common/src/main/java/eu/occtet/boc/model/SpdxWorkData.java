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

public class SpdxWorkData extends BaseWorkData{

    @JsonCreator
    public SpdxWorkData(@JsonProperty("jsonSpdx")String jsonSpdx,
                        @JsonProperty("bucketName")String bucketName,
                        @JsonProperty("projectId")String projectId,
                        @JsonProperty("useCopyrightAi")boolean useCopyrightAi,
                        @JsonProperty("useLicenseMatcher")boolean useLicenseMatcher) {
        this.jsonSpdx=jsonSpdx;
        this.bucketName=bucketName;
        this.projectId=projectId;
        this.useCopyrightAi = useCopyrightAi;
        this.useLicenseMatcher=useLicenseMatcher;
    }

    public SpdxWorkData() { }

    private String jsonSpdx;

    private String bucketName;

    private String projectId;

    boolean useCopyrightAi;

    boolean useLicenseMatcher;

    private byte[] jsonBytes;

    public String getJsonSpdx() {
        return jsonSpdx;
    }

    public void setJsonSpdx(String jsonSpdx) {
        this.jsonSpdx = jsonSpdx;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
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

    @Override
    public boolean process(IWorkDataProcessor processor) {
        return processor.process(this);
    }
}
