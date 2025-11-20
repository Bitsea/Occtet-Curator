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
public class Package {
    @Id
    private String SPDXID;

    @ManyToOne
    @JoinColumn(name = "spdx_document_id")
    private SpdxDocumentRoot spdxDocument;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String downloadLocation;

    @Lob
    private String versionInfo;

    @Lob
    private String licenseConcluded;

    @Lob
    private String licenseDeclared;

    @Lob
    private String copyrightText;

    @ElementCollection
    @CollectionTable(name = "package_license_info_from_files", joinColumns = @JoinColumn(name = "package_id"))
    private List<String> licenseInfoFromFiles;

    @Embedded
    private PackageVerificationCode packageVerificationCode;

    @OneToMany(mappedBy = "spdxElementId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Annotation> annotations;

    @OneToMany(mappedBy = "package", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalRef> externalRefs;

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Checksum> checksums;

    public String getName() {
        return name;
    }

    public SpdxDocumentRoot getSpdxDocument() {
        return spdxDocument;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public List<Checksum> getChecksums() {
        return checksums;
    }

    public List<ExternalRef> getExternalRefs() {
        return externalRefs;
    }

    public PackageVerificationCode getPackageVerificationCode() {
        return packageVerificationCode;
    }

    public List<String> getLicenseInfoFromFiles() {
        return licenseInfoFromFiles;
    }

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

    public String getSPDXID() {
        return SPDXID;
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

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public void setChecksums(List<Checksum> checksums) {
        this.checksums = checksums;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    public void setExternalRefs(List<ExternalRef> externalRefs) {
        this.externalRefs = externalRefs;
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

    public void setPackageVerificationCode(PackageVerificationCode packageVerificationCode) {
        this.packageVerificationCode = packageVerificationCode;
    }

    public void setSPDXID(String SPDXID) {
        this.SPDXID = SPDXID;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }
}

