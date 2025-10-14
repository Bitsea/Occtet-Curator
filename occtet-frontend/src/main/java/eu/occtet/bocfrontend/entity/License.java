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

package eu.occtet.bocfrontend.entity;


import com.fasterxml.jackson.annotation.JsonView;
import com.google.gson.annotations.SerializedName;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.UUID;

@JmixEntity
@Table(name = "LICENSE")
@Entity
public class License {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @Column(name= "PRIORITY")
    private Integer priority;

    @SerializedName("licenseId")
    @Column(name= "LICENSE_TYPE")
    private String licenseType;

    @Column(name= "LICENSE_TEXT", columnDefinition = "TEXT")
    private String licenseText;

    @SerializedName("name")
    @Column(name= "LICENSE_NAME")
    private String licenseName;

    @Column(name = "DETAILS_URL")
    @JsonView(ApiView.LicenseView.class)
    private String detailsUrl;

    @Column(name="MODIFIED")
    private Boolean isModified;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "IS_SPDX")
    private Boolean isSpdx;

    public License(){}

    public License(String licenseType, String licenseText, String licenseName) {
        this.licenseType = licenseType;
        this.licenseText = licenseText;
        this.licenseName = licenseName;
    }

    public License(String licenseType, String licenseText) {
        this.licenseType = licenseType;
        this.licenseText = licenseText;
    }


    public License(String licenseType, String licenseText, Boolean modified) {
        this.licenseType = licenseType;
        this.licenseText = licenseText;
        this.isModified= modified;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public Boolean getIsModified() {
        return isModified;
    }

    public void setIsModified(Boolean modified) {
        this.isModified = modified;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public Boolean isModified() {
        return isModified;
    }

    public void setModified(Boolean modified) {
        isModified = modified;
    }

    public Boolean getModified() {
        return isModified;
    }

    public Boolean isCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public Boolean isSpdx() {
        return isSpdx;
    }

    public void setSpdx(Boolean spdx) {
        isSpdx = spdx;
    }

    public Boolean getCurated() {
        return curated;
    }

    public Boolean getSpdx() {
        return isSpdx;
    }
}

