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
import java.util.List;

@Entity
public class ExtractedLicensingInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id", nullable = false)
    private SpdxDocumentRoot spdxDocument;

    @Lob
    @Column(nullable = false)
    private String extractedText;

    @Column(nullable = false)
    private String licenseId;

    private String name;

    @Lob
    private String comment;

    @ElementCollection
    @CollectionTable(name = "extracted_license_see_also", joinColumns = @JoinColumn(name = "license_id"))
    private List<String> seeAlsos;

    @ElementCollection
    @CollectionTable(name = "extracted_license_cross_ref", joinColumns = @JoinColumn(name = "license_id"))
    private List<CrossRef> crossRefs; // Collection of the CrossRef embeddable

    public String getComment() {
        return comment;
    }

    public Long getId() {
        return id;
    }

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public String getName() {
        return name;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public List<CrossRef> getCrossRefs() {
        return crossRefs;
    }

    public List<String> getSeeAlsos() {
        return seeAlsos;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public void setCrossRefs(List<CrossRef> crossRefs) {
        this.crossRefs = crossRefs;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public void setSeeAlsos(List<String> seeAlsos) {
        this.seeAlsos = seeAlsos;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
