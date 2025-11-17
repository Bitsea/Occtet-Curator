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

package eu.occtet.boc.entity.spdx2_3;

import jakarta.persistence.*;
import jdk.jshell.Snippet;

import java.util.List;

@Entity
public class SpdxDocumentRoot {

    @Id
    private String SPDXID;

    @Column(nullable = false)
    private String spdxVersion;

    @Column(nullable = false)
    private String dataLicense;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String documentNamespace;

    @Lob
    private String comment;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "creation_info_id", referencedColumnName = "id")
    private CreationInfo creationInfo;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalDocumentRef> externalDocumentRefs;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedLicensingInfo> hasExtractedLicensingInfos;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Relationship> relationships;

    // Other elements stored in the document
    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Package> packages;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpdxFile> files;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Snippet> snippets;

    public String getSPDXID() {
        return SPDXID;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public CreationInfo getCreationInfo() {
        return creationInfo;
    }

    public String getDataLicense() {
        return dataLicense;
    }

    public List<ExternalDocumentRef> getExternalDocumentRefs() {
        return externalDocumentRefs;
    }

    public List<ExtractedLicensingInfo> getHasExtractedLicensingInfos() {
        return hasExtractedLicensingInfos;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public String getDocumentNamespace() {
        return documentNamespace;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public List<SpdxFile> getFiles() {
        return files;
    }

    public List<Snippet> getSnippets() {
        return snippets;
    }

    public String getSpdxVersion() {
        return spdxVersion;
    }

    public void setCreationInfo(CreationInfo creationInfo) {
        this.creationInfo = creationInfo;
    }

    public void setDocumentNamespace(String documentNamespace) {
        this.documentNamespace = documentNamespace;
    }

    public void setDataLicense(String dataLicense) {
        this.dataLicense = dataLicense;
    }

    public void setSpdxVersion(String spdxVersion) {
        this.spdxVersion = spdxVersion;
    }

    public void setExternalDocumentRefs(List<ExternalDocumentRef> externalDocumentRefs) {
        this.externalDocumentRefs = externalDocumentRefs;
    }

    public void setHasExtractedLicensingInfos(List<ExtractedLicensingInfo> hasExtractedLicensingInfos) {
        this.hasExtractedLicensingInfos = hasExtractedLicensingInfos;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }

    public void setSnippets(List<Snippet> snippets) {
        this.snippets = snippets;
    }

    public void setFiles(List<SpdxFile> files) {
        this.files = files;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSPDXID(String SPDXID) {
        this.SPDXID = SPDXID;
    }
}
