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

package eu.occtet.bocfrontend.entity;


import com.fasterxml.jackson.annotation.JsonView;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JmixEntity
@Table(name = "SOFTWARE_COMPONENT")
@Entity
public class SoftwareComponent {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private Long id;

    @Column(name = "SOFTWARE_COMPONENT_NAME", nullable = false)
    @JsonView(ApiView.SoftwareComponentView.class)
    @InstanceName
    private String name;

    @Column(name = "VERSION", nullable = false)
    @JsonView(ApiView.SoftwareComponentView.class)
    private String version;

    @Column(name = "PURL")
    private String purl;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "LICENSE_AI_CONTROLLED")
    private Boolean licenseAiControlled;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "SOFTWARE_COMPONENT_LICENSE_LINK",
            joinColumns = @JoinColumn(name = "SOFTWARE_COMPONENT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "LICENSE_ID", referencedColumnName = "ID"))
    private List<License> licenses = new ArrayList<>();

    @OneToMany(mappedBy = "softwareComponent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(DeletePolicy.CASCADE)
    private List<ComponentVulnerabilityLink> vulnerabilityLinks = new ArrayList<>();

    @Column(name = "DETAILS_URL")
    private String detailsUrl;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name= "SOFTWARE_COMPONENT_ID")
    private List<Copyright> copyrights = new ArrayList<>();


    public SoftwareComponent(){
        this.licenseAiControlled= false;
    }

    public SoftwareComponent(String name, String version, String purl,
                             Boolean curated, List<License> licenses) {
        this.name = name;
        this.version = version;
        this.purl = purl;
        this.curated = curated;
        this.licenses = licenses;
        this.licenseAiControlled= false;
    }

    public Boolean getLicenseAiControlled() {
        return licenseAiControlled;
    }

    public void setLicenseAiControlled(Boolean aiControlled) {
        this.licenseAiControlled = aiControlled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getCurated() {
        return curated;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public void setPurl(String purl) {this.purl = purl;}

    public String getPurl() {return purl;}

    public Boolean isCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public List<Vulnerability> getVulnerabilities() {
        if (this.vulnerabilityLinks == null) {
            return new ArrayList<>();
        }
        return this.vulnerabilityLinks.stream()
                .map(ComponentVulnerabilityLink::getVulnerability)
                .collect(Collectors.toList());
    }

    /**
     * realtion between vulnerabilites and softwarecomponent is set with this link table, this new entity of link table
     * must be saved after the usage of this method, that is why this setter gives something back
     * better to implement this logic in the code his is used
     * @param vulnerabilities
     * @return
     */
    public List<ComponentVulnerabilityLink> setVulnerabilities(List<Vulnerability> vulnerabilities) {
        if (this.vulnerabilityLinks == null) {
            this.vulnerabilityLinks = new ArrayList<>();
        }

        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            this.vulnerabilityLinks.clear();
            return null;
        }

        this.vulnerabilityLinks.removeIf(link -> !vulnerabilities.contains(link.getVulnerability()));

        List<Vulnerability> existingVulnerabilities = this.vulnerabilityLinks.stream()
                .map(ComponentVulnerabilityLink::getVulnerability)
                .toList();

        for (Vulnerability vulnerability : vulnerabilities) {
            if (!existingVulnerabilities.contains(vulnerability)) {
                ComponentVulnerabilityLink newLink = new ComponentVulnerabilityLink();
                newLink.setSoftwareComponent(this);
                newLink.setVulnerability(vulnerability);
                newLink.setResolved(false);
                this.vulnerabilityLinks.add(newLink);
            }

        }
        //this must be saved in dataManager afterward
        return vulnerabilityLinks;
    }

    public List<ComponentVulnerabilityLink> getVulnerabilityLinks() {
        return vulnerabilityLinks;
    }

    public void setVulnerabilityLinks(List<ComponentVulnerabilityLink> vulnerabilityLinks) {
        this.vulnerabilityLinks = vulnerabilityLinks;
    }

    public List<Copyright> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(List<Copyright> copyrights) {
        this.copyrights = copyrights;
    }
}
