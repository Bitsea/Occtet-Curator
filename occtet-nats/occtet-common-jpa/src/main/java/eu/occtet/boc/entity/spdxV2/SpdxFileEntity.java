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
import java.util.zip.Checksum;

@Entity
public class SpdxFileEntity {

    @Id
    private String SPDXID;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id")
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false)
    private String fileName;

    @OneToMany(mappedBy = "spdxFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecksumEntity> checksums;

    @ElementCollection
    @CollectionTable(name = "file_types", joinColumns = @JoinColumn(name = "spdx_file_id"))
    @Column(name = "file_type")
    private List<String> fileTypes;

    @Lob
    private String licenseConcluded;

    @ElementCollection
    @CollectionTable(name = "file_license_info", joinColumns = @JoinColumn(name = "spdx_file_id"))
    private List<String> licenseInfoInFiles;

    @Lob
    private String copyrightText;

    public String getSPDXID() {
        return SPDXID;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public String getLicenseConcluded() {
        return licenseConcluded;
    }

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public List<ChecksumEntity> getChecksums() {
        return checksums;
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

    public List<String> getLicenseInfoInFiles() {
        return licenseInfoInFiles;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }

    public void setLicenseInfoInFiles(List<String> licenseInfoInFiles) {
        this.licenseInfoInFiles = licenseInfoInFiles;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }

    public void setChecksums(List<ChecksumEntity> checksums) {
        this.checksums = checksums;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setSPDXID(String SPDXID) {
        this.SPDXID = SPDXID;
    }

    public void setLicenseConcluded(String licenseConcluded) {
        this.licenseConcluded = licenseConcluded;
    }
}
