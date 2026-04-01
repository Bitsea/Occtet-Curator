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


import eu.occtet.boc.entity.appconfigurations.SearchTermsProfile;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;


@Entity
@Table(name = "PROJECT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ORGANIZATION_ID", "PROJECT_NAME", "VERSION"})
})
@EntityListeners(AuditingEntityListener.class)
public class Project implements HasOrganization {

    @Id
    @Column(name="ID", nullable = false, columnDefinition = "BIGINT")
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name="PROJECT_NAME")
    private String projectName;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "SPDX_DOCUMENT_ID", columnDefinition = "TEXT")
    private String documentID;

    @JoinTable(name = "PROJECT_SEARCH_TERMS_PROFILES_LINK",
            joinColumns = @JoinColumn(name = "PROJECT_ID"),
            inverseJoinColumns = @JoinColumn(name = "SEARCH_TERMS_PROFILE_ID"))
    @ManyToMany
    private List<SearchTermsProfile> searchTermsProfiles;

    @Column(name = "PROJECT_CONTACT", columnDefinition = "VARCHAR(255)", nullable = false)
    private String projectContact;

    @Column(name = "CONTACT_EMAIL", columnDefinition = "VARCHAR(255)")
    private String contactEmail;


    @Column(name = "CREATED_AT", updatable = false)
    private @Nonnull LocalDateTime createdAt;

    @OneToMany(mappedBy = "project",cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<File> files= new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID", nullable = false)
    private Organization organization;

    public Project() {this.createdAt = LocalDateTime.now();}

    public Project(String projectName ) {
        this.createdAt = LocalDateTime.now();
        this.projectName = projectName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDocumentID() {return documentID;}

    public void setDocumentID(String documentID) {this.documentID = documentID;}

    public List<SearchTermsProfile> getSearchTermsProfiles() {
        return searchTermsProfiles;
    }

    public void setSearchTermsProfiles(List<SearchTermsProfile> searchTermsProfiles) {
        this.searchTermsProfiles = searchTermsProfiles;
    }

    public String getProjectContact() {
        return projectContact;
    }

    public void setProjectContact(String projectContact) {
        this.projectContact = projectContact;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getVersion() {return version;}

    public void setVersion(String version) {this.version = version;}

    @Nonnull
    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(@Nonnull LocalDateTime createdAt) {this.createdAt = createdAt;}

    public Set<File> getFiles() {
        return files;
    }

    public void addFile(File file) {
        this.files.add(file);
        file.setProject(this);
    }

    public void addFiles(Collection<File> files) {
        this.files.addAll(files);
        for(File f : files){
            f.setProject(this);
        }
    }

    public void removeFiles() {
        this.files= new HashSet<>();
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

}
