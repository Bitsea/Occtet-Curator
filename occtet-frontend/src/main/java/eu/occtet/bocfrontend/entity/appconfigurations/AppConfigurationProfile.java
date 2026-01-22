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

package eu.occtet.bocfrontend.entity.appconfigurations;

import eu.occtet.bocfrontend.converter.StringListJsonbConverter;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@JmixEntity
@Table(name = "APP_CONFIGURATION_PROFILE")
@Entity
public class AppConfigurationProfile {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinColumn(name = "PROFILE_ID")
    @OneToMany(fetch = FetchType.LAZY)
    private List<Project> projects;

    @Column(name = "SEARCH_TERMS", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonbConverter.class)
    private List<String> searchTerms = new ArrayList<>();

    // add more if needed in the future, which is also mainly the reason of the naming

    public AppConfigurationProfile() {
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

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }
}