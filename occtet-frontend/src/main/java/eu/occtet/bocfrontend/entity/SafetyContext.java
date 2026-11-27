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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

@JmixEntity
@Table(name = "SAFETY_CONTEXT")
@Entity
public class SafetyContext {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOFTWARE_COMPONENT_ID")
    @JsonIgnore
    private SoftwareComponent softwareComponent;

    @Column(name="SAFETY_RELEVANT")
    private Boolean safetyRelevant;

    @Column(name="SAFETY_CATEGORY")
    private String safetyCategory;

    @Column(name="ASSURANCE_LEVEL")
    private String assuranceLevel;

    @Column(name = "RATIONALE", columnDefinition = "TEXT")
    private String rationale;

    @Column(name="SOURCE")
    private String source;

    @Column(name = "SOURCE_DOCUMENT", columnDefinition = "TEXT")
    private String sourceDocument;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SoftwareComponent getSoftwareComponent() {
        return softwareComponent;
    }

    public void setSoftwareComponent(SoftwareComponent softwareComponent) {
        this.softwareComponent = softwareComponent;
    }

    public Boolean getSafetyRelevant() {
        return safetyRelevant;
    }

    public void setSafetyRelevant(Boolean safetyRelevant) {
        this.safetyRelevant = safetyRelevant;
    }

    public String getSafetyCategory() {
        return safetyCategory;
    }

    public void setSafetyCategory(String safetyCategory) {
        this.safetyCategory = safetyCategory;
    }

    public String getAssuranceLevel() {
        return assuranceLevel;
    }

    public void setAssuranceLevel(String assuranceLevel) {
        this.assuranceLevel = assuranceLevel;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(String sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

}
