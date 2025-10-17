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

import java.util.UUID;

@Entity
@Table(name = "COPYRIGHT")
@EntityListeners(AuditingEntityListener.class)
public class Copyright {


    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "COPYRIGHT_TEXT", columnDefinition = "TEXT")
    private String copyrightText;

    @Column(name= "CURATED")
    private Boolean curated;

    @Column(name= "GARBAGE")
    private Boolean garbage;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "CODE_LOCATION_ID", columnDefinition = "TEXT")
    private CodeLocation codeLocation;

    public Copyright(String copyrightText, CodeLocation cl) {
        this.copyrightText = copyrightText;
        this.codeLocation = cl;
        this.curated = false;
        this.garbage = false;
    }

    public Copyright() {}

    public Copyright(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public CodeLocation getCodeLocation() {
        return codeLocation;
    }

    public void setCodeLocation(CodeLocation codeLocation) {
        this.codeLocation = codeLocation;
    }

    public boolean isGarbage() {
        return garbage;
    }

    public void setGarbage(boolean garbage) {
        this.garbage = garbage;
    }
}
