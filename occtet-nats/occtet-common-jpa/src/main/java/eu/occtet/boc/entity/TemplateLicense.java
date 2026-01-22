/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TEMPLATE_LICENSE")
@EntityListeners(AuditingEntityListener.class)
public class TemplateLicense {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "PRIORITY")
    private Integer priority;

    @Column(name = "LICENSE_TYPE")
    private String licenseType;

    @Column(name = "TEMPLATE_TEXT", columnDefinition = "TEXT")
    private String templateText;

    @Column(name = "LICENSE_NAME")
    private String licenseName;

    @Column(name = "DETAILS_URL")
    private String detailsUrl;

    @Column(name = "IS_SPDX")
    private Boolean isSpdx;

    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY)
    private List<UsageLicense> usages = new ArrayList<>();

    public TemplateLicense() {
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

    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public Boolean getIsSpdx() {
        return isSpdx;
    }

    public void setIsSpdx(Boolean isSpdx) {
        this.isSpdx = isSpdx;
    }

    public List<UsageLicense> getUsages() {
        return usages;
    }

    public void setUsages(List<UsageLicense> usages) {
        this.usages = usages;
    }



}
