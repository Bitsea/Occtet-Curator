/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JmixEntity
@Table(name = "ORGANIZATION")
@Entity
public class Organization {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ORGANIZATION_NAME", columnDefinition = "VARCHAR(255)", nullable = false)
    private String organizationName;

    @Column(name = "ORGANIZATION_EMAIL", columnDefinition = "VARCHAR(255)")
    private String organizationEmail;

    @OneToMany(mappedBy = "organization")
    @OnDelete(DeletePolicy.CASCADE)
    private Set<Project> projects = new HashSet<>();

    @OneToMany(mappedBy = "organization")
    @OnDelete(DeletePolicy.CASCADE)
    private Set<Vulnerability> vulnerabilities = new HashSet<>();

    @OneToMany(mappedBy = "organization")
    @OnDelete(DeletePolicy.CASCADE)
    private Set<SoftwareComponent> softwareComponents = new HashSet<>();

    @OneToMany(mappedBy = "organization")
    @OnDelete(DeletePolicy.CASCADE)
    private Set<User> users = new HashSet<>();


    public Organization() {
    }

    public Set<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(Set<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public Set<SoftwareComponent> getSoftwareComponents() {
        return softwareComponents;
    }

    public void setSoftwareComponents(Set<SoftwareComponent> softwareComponents) {
        this.softwareComponents = softwareComponents;
    }


    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationEmail() {
        return organizationEmail;
    }

    public void setOrganizationEmail(String organizationEmail) {
        this.organizationEmail = organizationEmail;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = projects;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
