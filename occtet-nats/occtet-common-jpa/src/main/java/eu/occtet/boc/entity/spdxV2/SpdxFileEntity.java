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
@Table(name ="SPDX_FILE_ENTITY")
public class SpdxFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, name="SPDX_ID")
    private String spdxId;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id")
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false, name="FILE_NAME")
    private String fileName;

    @OneToMany(mappedBy = "spdxFileEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecksumEntity> checksums;

    @ElementCollection
    @CollectionTable(name = "file_types", joinColumns = @JoinColumn(name = "spdx_file_id"))
    @Column(name = "file_type")
    private List<String> fileTypes;

    @Column(name= "license_concluded", columnDefinition = "TEXT")
    private String licenseConcluded;

    @ElementCollection
    @CollectionTable(name = "file_license_info", joinColumns = @JoinColumn(name = "spdx_file_id"))
    @Column(name = "license_info_in_files", columnDefinition = "TEXT")
    private List<String> licenseInfoInFiles;

    @Lob
    @Column(name = "copyright_text")
    private String copyrightText;

    public String getSpdxId() {
        return spdxId;
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

    public void setSpdxId(String SPDXID) {
        this.spdxId = SPDXID;
    }

    public void setLicenseConcluded(String licenseConcluded) {
        this.licenseConcluded = licenseConcluded;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SpdxFileEntity{" +
                "id=" + id +
                ", spdxId='" + spdxId + '\'' +
                ", spdxDocument=" + spdxDocument +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
