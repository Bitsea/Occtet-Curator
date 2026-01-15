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
@Table(name = "EXTERNAL_REF_DOCUMENT_ENTITY")
public class ExternalDocumentRefEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id", nullable = false)
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false, name="external_document_id")
    private String externalDocumentId;

    @Column(nullable = false, name = "spdx_document_external", length = 2048)
    private String spdxDocumentExternal;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "checksum_id", referencedColumnName = "id", nullable = false)
    private ChecksumEntity checksumEntity;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public String getExternalDocumentId() {
        return externalDocumentId;
    }

    public String getSpdxDocumentExternal() {
        return spdxDocumentExternal;
    }

    public void setExternalDocumentId(String externalDocumentId) {
        this.externalDocumentId = externalDocumentId;
    }

    public ChecksumEntity getChecksum() {
        return checksumEntity;
    }

    public void setChecksum(ChecksumEntity checksumEntity) {
        this.checksumEntity = checksumEntity;
    }

    public void setSpdxDocumentExternal(String spdxDocumentExternal) {
        this.spdxDocumentExternal = spdxDocumentExternal;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }
}
