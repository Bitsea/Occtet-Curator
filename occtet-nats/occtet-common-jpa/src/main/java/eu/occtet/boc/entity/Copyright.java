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


import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "COPYRIGHT")
@EntityListeners(AuditingEntityListener.class)
public class Copyright {


    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "COPYRIGHT_TEXT",columnDefinition = "TEXT")
    private String copyrightText;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "GARBAGE")
    private Boolean garbage;

    @Column(name= "AI_CONTROLLED")
    private Boolean aiControlled;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinTable(
            name = "COPYRIGHT_FILE_LINK",
            joinColumns = @JoinColumn(name = "COPYRIGHT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "FILE_ID", referencedColumnName = "ID"))
    private Set<File> files = new HashSet<>();;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name= "COPYRIGHT_ID")
    private List<License> licenses;

    public Copyright(String copyrightText, Set<File> cl) {
        this.copyrightText = copyrightText;
        this.files = cl;
        this.curated = false;
        this.garbage = false;
        this.aiControlled=false;
    }

    public Boolean getAiControlled() {
        return aiControlled;
    }

    public void setAiControlled(Boolean aiControlled) {
        this.aiControlled = aiControlled;
    }

    public Copyright() {}

    public Copyright(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public void setCopyrightText(String copyrightString) {
        this.copyrightText = copyrightString;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    public Set<File> getFiles() {
        return files;
    }

    public void setFiles(Set<File> files) {
        this.files = files;
    }

    public boolean isGarbage() {
        return garbage;
    }

    public void setGarbage(boolean garbage) {
        this.garbage = garbage;
    }

    public Boolean getCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public Boolean getGarbage() {
        return garbage;
    }

    public void setGarbage(Boolean garbage) {
        this.garbage = garbage;
    }
}
