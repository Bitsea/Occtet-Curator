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

package eu.occtet.bocfrontend.entity;


import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;


import java.util.UUID;

@JmixEntity
@Table(name = "COPYRIGHT")
@Entity
public class Copyright {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @InstanceName
    @Column(name = "COPYRIGHT_TEXT")
    private String copyrightText;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "GARBAGE")
    private Boolean garbage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "CODE_LOCATION_ID")
    private CodeLocation codeLocation;

    public UUID getId() {return id;}

    public void setId(UUID id) {this.id = id;}

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

    public CodeLocation getCodeLocation(){return this.codeLocation;}

    public void setCodeLocation(CodeLocation codeLocation) {this.codeLocation = codeLocation;}

    public Boolean getCurated() {
        return curated;
    }

    public Boolean getGarbage() {
        return garbage;
    }
}

