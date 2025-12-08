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

    private String url;
    private String location;
    private String version;
    private String projectId;
    private Boolean isMainPackage;

    @JsonCreator
    public DownloadServiceWorkData(@JsonProperty("url") String url,@JsonProperty("location") String location
            ,@JsonProperty("version") String version, @JsonProperty("projectId") String projectId,
                                   @JsonProperty("isMainPackage") Boolean isMainPackage) {

        this.url = url;
        this.location = location;
        this.version = version;
        this.projectId = projectId;
        this.isMainPackage = isMainPackage;
    }

    public void setUrl(String url){this.url = url;}
    public void setLocation(String location){this.location = location;}
    public void setVersion(String version){this.version = version;}
    public void setProjectId(String projectId){this.projectId = projectId;}
    public void setIsMainPackage(Boolean isMainPackage){this.isMainPackage = isMainPackage;}
    public String getUrl(){return this.url;}
    public String getLocation(){return this.location;}
    public String getVersion(){return this.version;}
    public String getProjectId(){return this.projectId;}
    public Boolean getIsMainPackage(){return this.isMainPackage;}

    @Override
    public boolean process(IWorkDataProcessor processor) {return processor.process(this);}
}
