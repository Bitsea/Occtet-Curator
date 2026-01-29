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


import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@JmixEntity
@Table(name = "COPYRIGHT")
@Entity
public class Copyright {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @InstanceName
    @Column(name = "COPYRIGHT_TEXT", columnDefinition = "TEXT")
    private String copyrightText;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "AI_CONTROLLED")
    private Boolean aiControlled;

    @Column(name= "GARBAGE")
    private Boolean garbage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "COPYRIGHT_CODE_LOCATION_LINK",
            joinColumns = @JoinColumn(name = "COPYRIGHT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "CODE_LOCATION_ID", referencedColumnName = "ID"))
    @OnDelete(DeletePolicy.CASCADE)
    private Set<CodeLocation> codeLocations = new HashSet<>();;


    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name= "COPYRIGHT_ID")
    private List<License> licenses;

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public String getCopyrightText() {return copyrightText;}

    public void setCopyrightText(String copyrightText) {this.copyrightText = copyrightText;}

    public Boolean isCurated() {
        return curated;
    }

    public void setCurated(Boolean curated) {
        this.curated = curated;
    }

    public Boolean isGarbage() {return garbage;}

    public void setGarbage(Boolean garbage) {this.garbage = garbage;}

    public Set<CodeLocation> getCodeLocations(){return this.codeLocations;}

    public void setCodeLocations(Set<CodeLocation> codeLocations) {this.codeLocations = codeLocations;}

    public Boolean getCurated() {
        return curated;
    }

    public Boolean getGarbage() {
        return garbage;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public Boolean getAiControlled() {
        return aiControlled;
    }

    public void setAiControlled(Boolean aiControlled) {
        this.aiControlled = aiControlled;
    }
}

