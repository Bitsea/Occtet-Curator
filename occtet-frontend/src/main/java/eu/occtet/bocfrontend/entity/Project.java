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


import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "PROJECT")
@Entity
public class Project {

    @JmixGeneratedValue
    @Id
    @Column(name="ID", nullable = false)
    private UUID id;

    @Column(name="PROJECT_NAME")
    private String projectName;


    public Project() {}

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public UUID getId() {return id;}

    public void setId(UUID id) {this.id = id;}

    public String getProjectName() {return projectName;}

    public void setProjectName(String projectName) {this.projectName = projectName;}

}

