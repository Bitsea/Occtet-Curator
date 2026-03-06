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

package eu.occtet.bocfrontend.entity;


import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

@JmixEntity
@Table(name = "ORT_VIOLATION")
@Entity
public class OrtViolation {

    @Id
    @Column(name="ID", nullable = false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name= "MESSAGE",columnDefinition = "TEXT")
    private String message;

    @Column(name="RULE", columnDefinition = "TEXT")
    private String rule;

    @Column(name="SEVERITY")
    private String severity;

    @Column(name="PURL")
    private String purl;

    @Column(name="HOW_TO_FIX", columnDefinition = "TEXT")
    private String howToFix;

    @Column(name="LICENSE")
    private String license;

    @Column(name="LICENSE_SOURCE")
    private String licenseSource;

    @Column(name="RESOLVED")
    private Boolean resolved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @OnDelete(DeletePolicy.CASCADE)
    private Project project;

    @ManyToOne
    @JoinColumn(name="INVENTORY_ITEM_ID")
    private InventoryItem inventoryItem;


    public OrtViolation() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public String getHowToFix() {
        return howToFix;
    }

    public void setHowToFix(String howToFix) {
        this.howToFix = howToFix;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseSource() {
        return licenseSource;
    }

    public void setLicenseSource(String licenseSource) {
        this.licenseSource = licenseSource;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
}
