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

public class MicroserviceDescriptor  extends BaseSystemMessage {
    private String name, description,version,acceptableWorkData;
    private UsageType usageType;

    public MicroserviceDescriptor() {
    }

    public MicroserviceDescriptor(String name, String description, String version, String acceptableWorkData, UsageType usageType) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.acceptableWorkData = acceptableWorkData;
        this.usageType = usageType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAcceptableWorkData() {
        return acceptableWorkData;
    }

    public void setAcceptableWorkData(String acceptableWorkData) {
        this.acceptableWorkData = acceptableWorkData;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }
}
