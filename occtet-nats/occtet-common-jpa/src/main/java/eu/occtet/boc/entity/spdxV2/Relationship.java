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
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id")
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false)
    private String spdxElementId;

    @Column(nullable = false)
    private String relatedSpdxElement;

    @Column(nullable = false)
    private String relationshipType;

    @Lob
    private String comment;

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public Long getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public String getSpdxElementId() {
        return spdxElementId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getRelatedSpdxElement() {
        return relatedSpdxElement;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setSpdxElementId(String spdxElementId) {
        this.spdxElementId = spdxElementId;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }

    public void setRelatedSpdxElement(String relatedSpdxElement) {
        this.relatedSpdxElement = relatedSpdxElement;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
}
