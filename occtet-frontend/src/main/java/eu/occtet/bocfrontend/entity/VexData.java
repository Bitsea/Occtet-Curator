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


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "VEX_DATA")
@Entity
public class VexData{

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @Column(name="TITLE")
    private String title;

    @Column(name="CHECKED")
    private Boolean checked;

    @Column(name="BOM_FORMAT")
    private String bomFormat;

    @Column(name="SPEC_VERSION")
    private String specVersion;

    @Column(name="SERIAL_NUMBER")
    private String serialNumber;

    @Column(name="VERSION")
    private Integer version;

    @Column(name="TIME_STAMP")
    private @Nonnull LocalDateTime timeStamp;

    @JoinTable(name = "VULNERABILITY_VEX_LINK", joinColumns = @JoinColumn(name = "VEX_DATA_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "VULNERABILITY_ID", referencedColumnName = "ID"))
    @ManyToMany(cascade = {CascadeType.REFRESH})
    private List<Vulnerability> vulnerability;

    @Column(name = "META_DATA", columnDefinition = "TEXT")
    private String metaData;

    @Column(name = "ANALYSIS_DATA", columnDefinition = "TEXT")
    private String analysisData;

    @Column(name = "VULNERABILITY_DATA", columnDefinition = "TEXT")
    private String vulnerabilityData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOFTWARE_COMPONENT_ID")
    private SoftwareComponent softwareComponent;


    public VexData() {
        this.timeStamp= LocalDateTime.now();
    }


    public SoftwareComponent getSoftwareComponent() {
        return softwareComponent;
    }

    public void setSoftwareComponent(SoftwareComponent softwareComponent) {
        this.softwareComponent = softwareComponent;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getAnalysisData() {
        return analysisData;
    }

    public void setAnalysisData(String analysisData) {
        this.analysisData = analysisData;
    }

    public String getVulnerabilityData() {
        return vulnerabilityData;
    }

    public void setVulnerabilityData(String vulnerabilityData) {
        this.vulnerabilityData = vulnerabilityData;
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBomFormat() {
        return bomFormat;
    }

    public void setBomFormat(String bomFormat) {
        this.bomFormat = bomFormat;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Vulnerability> getVulnerability() {
        return vulnerability;
    }

    public void setVulnerability(List<Vulnerability> vulnerability) {
        this.vulnerability = vulnerability;
    }

    @Nonnull
    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

}
