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

public class InformationFileSendWorkData extends BaseWorkData{

    private String path;
    private String context;

    @JsonCreator
    public InformationFileSendWorkData(@JsonProperty("path") String path,
                                       @JsonProperty("context") String context){
        this.path = path;
        this.context = context;
    }

    public void setPath(String path) {this.path = path;}

    public String getPath() {return path;}

    public void setContext(String context) {this.context = context;}

    public String getContext() {return context;}

    @Override
    public boolean process(IWorkDataProcessor processor) {return processor.process(this);}
}
