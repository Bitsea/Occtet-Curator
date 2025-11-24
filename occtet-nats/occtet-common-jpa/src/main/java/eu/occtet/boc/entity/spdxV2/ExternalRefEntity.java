/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.entity.spdxV2;

import jakarta.persistence.*;

@Entity
public class ExternalRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package pkg;

    @Column(nullable = false)
    private String referenceCategory;

    @Column(nullable = false)
    private String referenceLocator;

    @Column(nullable = false)
    private String referenceType;

    @Lob
    private String comment;

    public Long getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public Package getPkg() {
        return pkg;
    }

    public String getReferenceCategory() {
        return referenceCategory;
    }

    public String getReferenceLocator() {
        return referenceLocator;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setPkg(Package pkg) {
        this.pkg = pkg;
    }

    public void setReferenceCategory(String referenceCategory) {
        this.referenceCategory = referenceCategory;
    }

    public void setReferenceLocator(String referenceLocator) {
        this.referenceLocator = referenceLocator;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}
