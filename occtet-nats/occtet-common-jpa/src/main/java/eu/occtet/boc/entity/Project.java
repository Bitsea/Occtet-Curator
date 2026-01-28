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

package eu.occtet.boc.entity;


import eu.occtet.boc.entity.appconfigurations.SearchTermsProfile;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;


@Entity
@Table(name = "PROJECT")
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @Column(name="ID", nullable = false, columnDefinition = "BIGINT")
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    private Long id;

    @Column(name="PROJECT_NAME")
    private String projectName;

    @Column(name = "SPDX_DOCUMENT_ID", columnDefinition = "TEXT")
    private String documentID;

    @JoinTable(name = "PROJECT_SEARCH_TERMS_PROFILES_LINK",
            joinColumns = @JoinColumn(name = "PROJECT_ID"),
            inverseJoinColumns = @JoinColumn(name = "SEARCH_TERMS_PROFILE_ID"))
    @ManyToMany
    private List<SearchTermsProfile> searchTermsProfiles;

    @Column(name = "PROJECT_CONTACT", columnDefinition = "VARCHAR(255)")
    private String projectContact;

    @Column(name = "CONTACT_EMAIL", columnDefinition = "VARCHAR(255)")
    private String contactEmail;

    public Project() {
    }

    public Project(String projectName ) {
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
}
