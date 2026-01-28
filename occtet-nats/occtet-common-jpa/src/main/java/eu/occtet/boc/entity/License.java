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

package eu.occtet.boc.entity;


import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;



@Entity
@Table(name = "LICENSE")
@EntityListeners(AuditingEntityListener.class)
public class License {


    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name= "PRIORITY")
    private Integer priority;

    @Column(name= "LICENSE_TYPE")
    private String licenseType;

    @Column(name= "LICENSE_TEXT",columnDefinition = "TEXT")
    private String licenseText;

    @Column(name= "LICENSE_NAME")
    private String licenseName;

    @Column(name = "DETAILS_URL")
    private String detailsUrl;

    @Column(name="MODIFIED")
    private Boolean isModified;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "IS_SPDX")
    private Boolean isSpdx;

    public License() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
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

    public boolean getIsModified() {
        return isModified;
    }

    public void setIsModified(boolean modified) {
        this.isModified = modified;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        isModified = modified;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    public boolean isSpdx() {
        return isSpdx;
    }

    public void setSpdx(boolean spdx) {
        isSpdx = spdx;
    }

}
