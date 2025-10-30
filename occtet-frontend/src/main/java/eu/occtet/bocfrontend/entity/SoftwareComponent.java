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
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;

@JmixEntity
@Table(name = "SOFTWARE_COMPONENT")
@Entity
public class SoftwareComponent {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "SOFTWARE_COMPONENT_LICENSE_LINK",
            joinColumns = @JoinColumn(name = "SOFTWARE_COMPONENT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "LICENSE_ID", referencedColumnName = "ID"))
    private List<License> licenses;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "SOFTWARE_COMPONENT_VULNERABILITY_LINK",
            joinColumns = @JoinColumn(name = "SOFTWARE_COMPONENT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "VULNERABILITY_ID", referencedColumnName = "ID"))
    @OnDelete(DeletePolicy.CASCADE)
    private List<Vulnerability> vulnerabilities;

    @Column(name = "DETAILS_URL")
    private String detailsUrl;


    public SoftwareComponent(){}

    public SoftwareComponent(String name, String version, String purl,
                             Boolean curated, List<License> licenses) {
        this.name = name;
        this.version = version;
        this.purl = purl;
        this.curated = curated;
        this.licenses = licenses;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
        return vulnerabilities;
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}
