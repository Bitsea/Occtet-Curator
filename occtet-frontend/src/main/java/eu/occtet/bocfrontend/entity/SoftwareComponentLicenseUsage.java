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

package eu.occtet.bocfrontend.entity;


import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@JmixEntity
@Table(name = "SOFTWARE_COMPONENT_LICENSE_USAGE")
@Entity
public class SoftwareComponentLicenseUsage implements HasOrganization {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "CUSTOM_NAME", columnDefinition = "TEXT")
    private String customName;


    @Column(name = "USAGE_TEXT", columnDefinition = "TEXT")
    private String usageText;

    @Column(name = "MODIFIED")
    private Boolean isModified;

    @Column(name = "CURATED")
    private Boolean curated;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOFTWARE_COMPONENT_ID")
    private SoftwareComponent softwareComponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LICENSE_ID")
    private License template;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID")
    private Organization organization;

    @ManyToMany(mappedBy = "licenses")
    @OnDelete(DeletePolicy.UNLINK)
    private Set<Copyright> copyrights = new HashSet<>();

    @JmixProperty
    @DependsOnProperties({"usageText", "template"})
    public String getEffectiveText() {
        //usagetext has precedence over template text
        if (usageText != null && !usageText.isBlank()) {
            return usageText;
        }
        return template.getTemplateText();
    }

    @JmixProperty
    @DependsOnProperties({"template"})
    public String getEffectiveName() {
        return (customName != null) ? customName: template.getLicenseName();
    }


    public Long getId() {
        return id;
    }

    public String getUsageText() {
        return usageText;
    }

    public void setUsageText(String usageText) {
        this.usageText = usageText;
    }

    public Boolean getIsModified() {
        return isModified;
    }

    public void setIsModified(Boolean modified) {
        isModified = modified;
    }

    public Boolean getCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public SoftwareComponent getSoftwareComponent() {
        return softwareComponent;
    }

    public void setSoftwareComponent(SoftwareComponent softwareComponent) {
        this.softwareComponent = softwareComponent;
    }

    public void setTemplate(License template) {
        this.template = template;
    }

    public License getTemplate() {
        return template;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public Set<Copyright> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(Set<Copyright> copyrights) {
        this.copyrights = copyrights;
    }

    @Override
    public Organization getOrganization() {
        return organization;
    }

    @Override
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

}
