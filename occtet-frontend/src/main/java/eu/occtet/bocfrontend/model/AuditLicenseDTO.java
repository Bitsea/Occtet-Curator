/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package eu.occtet.bocfrontend.model;

import io.jmix.core.metamodel.annotation.JmixEntity;

@JmixEntity
public class AuditLicenseDTO {

    private String licenseName;

    private Integer numSc;

    public AuditLicenseDTO(){}

    public AuditLicenseDTO(String licenseName, Integer numSc){
        this.licenseName = licenseName;
        this.numSc = numSc;
    }

    public void setLicenseName(String licenseName) {this.licenseName = licenseName;}
    public String getLicenseName() {return licenseName;}
    public void setNumSc(Integer numSc) {this.numSc = numSc;}
    public Integer getNumSc() {return numSc;}
}
