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
@Table(name = "SPDX_PACKAGE_ENTITY")
public class SpdxPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "spdx_id", nullable = false)
    private String spdxId;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id")
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, name="download_location")
    private String downloadLocation;

    @Column(name="files_analyzed")
    private boolean filesAnalyzed;

    @Column(name="version_info",length = Integer.MAX_VALUE)
    private String versionInfo;

    @Column(name="license_concluded",length = Integer.MAX_VALUE)
    private String licenseConcluded;

    @Column(name="license_declared",length = Integer.MAX_VALUE)
    private String licenseDeclared;

    @Column(name="copyright_text",length = Integer.MAX_VALUE)
    private String copyrightText;

    @Column(length = Integer.MAX_VALUE)
    private String homepage;

    @Column(length = Integer.MAX_VALUE)
    private String summary;

    @Column(length = Integer.MAX_VALUE)
    private String description;

    @Column(length = Integer.MAX_VALUE)
    private String originator;

    @Column(length = Integer.MAX_VALUE)
    private String supplier;

    @ElementCollection
    @CollectionTable(name = "package_license_info_from_files", joinColumns = @JoinColumn(name = "package_id"))
    private List<String> licenseInfoFromFiles;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "package_verification_code_id")
    private PackageVerificationCodeEntity packageVerificationCodeEntity;

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnnotationEntity> annotationEntities;

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalRefEntity> externalRefEntities;

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecksumEntity> checksumEntities;

    @ElementCollection
    @CollectionTable(name = "package_file_names", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "file_name")
    private List<String> fileNames;

    public String getName() {
        return name;
    }

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public List<AnnotationEntity> getAnnotations() {
        return annotationEntities;
    }

    public List<ChecksumEntity> getChecksums() {
        return checksumEntities;
    }

    public List<ExternalRefEntity> getExternalRefs() {
        return externalRefEntities;
    }

    public PackageVerificationCodeEntity getPackageVerificationCode() {
        return packageVerificationCodeEntity;
    }

    public List<String> getLicenseInfoFromFiles() {
        return licenseInfoFromFiles;
    }

    public List<String> getFileNames() {return fileNames;}

    public void setFileNames(List<String> fileNames) {this.fileNames = fileNames;}

    public String getDownloadLocation() {
        return downloadLocation;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public String getLicenseConcluded() {
        return licenseConcluded;
    }

    public String getLicenseDeclared() {
        return licenseDeclared;
    }

    public String getSpdxId() {
        return spdxId;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setSpdxDocument(SpdxDocumentRoot spdxDocument) {
        this.spdxDocument = spdxDocument;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAnnotations(List<AnnotationEntity> annotationEntities) {
        this.annotationEntities = annotationEntities;
    }

    public void setChecksums(List<ChecksumEntity> checksumEntities) {
        this.checksumEntities = checksumEntities;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    public void setExternalRefs(List<ExternalRefEntity> externalRefEntities) {
        this.externalRefEntities = externalRefEntities;
    }

    public void setLicenseConcluded(String licenseConcluded) {
        this.licenseConcluded = licenseConcluded;
    }

    public void setLicenseDeclared(String licenseDeclared) {
        this.licenseDeclared = licenseDeclared;
    }

    public void setLicenseInfoFromFiles(List<String> licenseInfoFromFiles) {
        this.licenseInfoFromFiles = licenseInfoFromFiles;
    }

    public void setPackageVerificationCode(PackageVerificationCodeEntity packageVerificationCodeEntity) {
        this.packageVerificationCodeEntity = packageVerificationCodeEntity;
    }

    public void setSpdxId(String SPDXID) {
        this.spdxId = SPDXID;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public Long getId() {
        return id;
    }

    public boolean isFilesAnalyzed() {
        return filesAnalyzed;
    }

    public void setFilesAnalyzed(boolean filesAnalyzed) {
        this.filesAnalyzed = filesAnalyzed;
    }

    public String getDescription() {
        return description;
    }

    public String getSummary() {
        return summary;
    }

    public String getHomepage() {
        return homepage;
    }

    public String getOriginator() {
        return originator;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}


