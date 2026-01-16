/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.entity.settings.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "CONFIGURATION_PROFILE")
@Entity
public class ConfigurationProfile {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinColumn(name = "PROJECT_ID")
    @OneToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "SEARCH_TERMS", columnDefinition = "TEXT")
    private String jsonSearchTerms;

    @JmixProperty
    @Transient
    public String getSearchTermsInput() {
        if (jsonSearchTerms == null || jsonSearchTerms.isEmpty()) {
            return "";
        }
        try {
            List<String> list = MAPPER.readValue(jsonSearchTerms, List.class);
            return String.join("\n", list);
        } catch (Exception e) {
            return "";
        }
    }

    public void setSearchTermsInput(String input) {
        if (input == null || input.isEmpty()) {
            this.jsonSearchTerms = "[]";
            return;
        }
        String[] terms = input.split("\\R");

        try {
            this.jsonSearchTerms = MAPPER.writeValueAsString(terms);
        } catch (Exception e) {
            this.jsonSearchTerms = "[]";
        }
    }

    public ConfigurationProfile() {}

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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getJsonSearchTerms() {
        return jsonSearchTerms;
    }

    public void setJsonSearchTerms(String jsonSearchTerms) {
        this.jsonSearchTerms = jsonSearchTerms;
    }
}
