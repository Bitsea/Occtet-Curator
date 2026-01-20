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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SPDX_DOCUMENT_ROOT")
public class SpdxDocumentRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, name="SPDX_ID")
    private String spdxId;

    @Column(nullable = false, name="SPDX_VERSION")
    private String spdxVersion;

    @Column(nullable = false, name = "DATA_LICENSE")
    private String dataLicense;

    @Column(nullable = false, name="NAME")
    private String name;

    @Column(name="comment", columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, name = "DOCUMENT_URI")
    private String documentUri;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "creation_info_id", referencedColumnName = "id")
    private CreationInfoEntity creationInfoEntity;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalDocumentRefEntity> externalDocumentRefEntities;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedLicensingInfoEntity> hasExtractedLicensingInfoEntities;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RelationshipEntity> relationshipEntities;

    // Other elements stored in the document
    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpdxPackageEntity> spdxPackageEntities;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpdxFileEntity> files;

    @OneToMany(mappedBy = "spdxDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Snippet> snippets;

    public SpdxDocumentRoot(){
        externalDocumentRefEntities = new ArrayList<>();
        hasExtractedLicensingInfoEntities = new ArrayList<>();
        relationshipEntities = new ArrayList<>();
        spdxPackageEntities = new ArrayList<>();
        files = new ArrayList<>();
        snippets = new ArrayList<>();
    }

    public String getSpdxId() {
        return spdxId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public CreationInfoEntity getCreationInfo() {
        return creationInfoEntity;
    }

    public String getDataLicense() {
        return dataLicense;
    }

    public List<ExternalDocumentRefEntity> getExternalDocumentRefs() {
        return externalDocumentRefEntities;
    }

    public List<ExtractedLicensingInfoEntity> getHasExtractedLicensingInfos() {
        return hasExtractedLicensingInfoEntities;
    }

    public List<SpdxPackageEntity> getPackages() {
        return spdxPackageEntities;
    }

    public List<RelationshipEntity> getRelationships() {
        return relationshipEntities;
    }

    public List<SpdxFileEntity> getFiles() {
        return files;
    }

    public List<Snippet> getSnippets() {
        return snippets;
    }

    public String getSpdxVersion() {
        return spdxVersion;
    }

    public void setCreationInfo(CreationInfoEntity creationInfoEntity) {
        this.creationInfoEntity = creationInfoEntity;
    }

    public void setDataLicense(String dataLicense) {
        this.dataLicense = dataLicense;
    }

    public void setSpdxVersion(String spdxVersion) {
        this.spdxVersion = spdxVersion;
    }

    public void setExternalDocumentRefs(List<ExternalDocumentRefEntity> externalDocumentRefEntities) {
        this.externalDocumentRefEntities = externalDocumentRefEntities;
    }

    public void setHasExtractedLicensingInfos(List<ExtractedLicensingInfoEntity> hasExtractedLicensingInfoEntities) {
        this.hasExtractedLicensingInfoEntities = hasExtractedLicensingInfoEntities;
    }

    public void setPackages(List<SpdxPackageEntity> spdxPackageEntities) {
        this.spdxPackageEntities = spdxPackageEntities;
    }

    public void setRelationships(List<RelationshipEntity> relationshipEntities) {
        this.relationshipEntities = relationshipEntities;
    }

    public void setSnippets(List<Snippet> snippets) {
        this.snippets = snippets;
    }

    public void setFiles(List<SpdxFileEntity> files) {
        this.files = files;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpdxId(String SPDXID) {
        this.spdxId = SPDXID;
    }

    public String getDocumentUri() {return documentUri;}

    public void setDocumentUri(String documentUri) {this.documentUri = documentUri;}

    public Long getId() {
        return id;
    }
}
