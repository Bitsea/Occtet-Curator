/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.entity;


import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Entity
@Table(name = "SOFTWARE_COMPONENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ORGANIZATION_ID", "SOFTWARE_COMPONENT_NAME", "VERSION"})
})
@EntityListeners(AuditingEntityListener.class)
public class SoftwareComponent implements HasOrganization {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "SOFTWARE_COMPONENT_NAME", nullable = false)
    private String name;

    @Transient
    private String bomRef;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "PURL")
    private String purl;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "LICENSE_AI_CONTROLLED")
    private Boolean licenseAiControlled;

    @Column(name= "DETAILS_URL")
    private String detailsUrl;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name= "SOFTWARE_COMPONENT_ID")
    private List<Copyright> copyrights;

    @OneToMany(mappedBy = "softwareComponent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoftwareComponentLicenseUsage> usageLicenses = new ArrayList<>();

    @OneToMany(mappedBy = "softwareComponent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComponentVulnerabilityLink> vulnerabilityLinks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID", nullable = false)
    private Organization organization;

    public SoftwareComponent() {
    }

    public SoftwareComponent(String softwareName, String version,
                             List<SoftwareComponentLicenseUsage> license, Organization organization) {
        this.name = softwareName;
        this.version = version;
        this.usageLicenses = license;
        this.curated = false;
        this.organization= organization;
    }

    public SoftwareComponent(String softwareName, String version,
                             List<SoftwareComponentLicenseUsage> license, String url, Organization organization) {
        this.name = softwareName;
        this.version = version;
        this.usageLicenses = license;
        this.curated = false;
        this.detailsUrl= url;
        this.organization= organization;
    }

    public SoftwareComponent(String softwareName, String version, Organization organization){
        this.name = softwareName;
        this.version = version;
        this.curated = false;
        this.usageLicenses = new ArrayList<>();
        this.organization= organization;
    }
    public SoftwareComponent(
            String name,
            String version,
            String purl,
            boolean curated,
            List<SoftwareComponentLicenseUsage> usageLicenses,
            String url
    ) {
        this.name = name;
        this.version = version;
        this.purl = purl;
        this.curated = curated;
        this.usageLicenses = usageLicenses ==null? new ArrayList<>() : usageLicenses;
        this.detailsUrl= url;
    }

    public Boolean getLicenseAiControlled() {
        return licenseAiControlled;
    }

    public void setLicenseAiControlled(Boolean licenseAiControlled) {
        this.licenseAiControlled = licenseAiControlled;
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

    public List<SoftwareComponentLicenseUsage> getUsageLicenses() {
        return usageLicenses;
    }

    public void setUsageLicenses(List<SoftwareComponentLicenseUsage> usageLicenses) {
        this.usageLicenses = usageLicenses;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public Boolean getCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public List<Vulnerability> getVulnerabilities() {
        if (this.vulnerabilityLinks == null) {
            return new ArrayList<>();
        }
        return this.vulnerabilityLinks.stream()
                .map(ComponentVulnerabilityLink::getVulnerability)
                .collect(Collectors.toList());
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        if (this.vulnerabilityLinks == null) {
            this.vulnerabilityLinks = new ArrayList<>();
        }

        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            this.vulnerabilityLinks.clear();
            return;
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
    }





    public List<Copyright> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(List<Copyright> copyrights) {
        this.copyrights = copyrights;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void addLicenseUsage(SoftwareComponentLicenseUsage usage) {
        if (this.usageLicenses == null) {
            this.usageLicenses=new ArrayList<>();
        }
        this.usageLicenses.add(usage);
        usage.setSoftwareComponent(this);

        if (this.organization != null) {
            usage.setOrganization(this.organization);
        }
    }

    public void addVulnerabilityLink(ComponentVulnerabilityLink link) {
        if (this.vulnerabilityLinks == null) {
            this.vulnerabilityLinks= new ArrayList<>();
        }
        this.vulnerabilityLinks.add(link);
        link.setSoftwareComponent(this);

    }

    public void removeLicenseUsage(SoftwareComponentLicenseUsage usage) {
        if (this.usageLicenses != null) {
            this.usageLicenses.remove(usage);
            usage.setSoftwareComponent(null);
        }
    }

    public String getBomRef() {
        return bomRef;
    }

    public void setBomRef(String bomRef) {
        this.bomRef = bomRef;
    }

}
