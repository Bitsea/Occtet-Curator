/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.spdx.context;

import eu.occtet.boc.entity.*;

import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import lombok.Data;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;

import java.util.*;

@Data
public class SpdxImportContext {

    private final Project project;
    private final SpdxDocument spdxDocument;
    private final SpdxDocumentRoot spdxDocumentRoot;

    // Caches and State
    private Collection<ExtractedLicenseInfo> extractedLicenseInfos;
    private Map<String, SpdxPackageEntity> packageLookupMap = new HashMap<>();
    private Map<String, SoftwareComponent> componentCache = new HashMap<>();
    private Map<String, TemplateLicense> licenseCache = new HashMap<>();
    private Map<String, InventoryItem> fileToInventoryItemMap = new HashMap<>();
    private Map<String, InventoryItem> inventoryCache = new HashMap<>();

    // Tracking Sets
    private List<InventoryItem> inventoryItems = new ArrayList<>();
    private Set<Long> mainInventoryItems = new HashSet<>();
    private Set<String> mainPackageIds = new HashSet<>();
    private Set<String> processedFileIds = new HashSet<>();
    private Map<String, List<Relationship>> packageRelationships = new HashMap<>();

    public SpdxImportContext(Project project, SpdxDocument spdxDocument, SpdxDocumentRoot root) {
        this.project = project;
        this.spdxDocument = spdxDocument;
        this.spdxDocumentRoot = root;
    }

    public Project getProject() {
        return project;
    }

    public SpdxDocument getSpdxDocument() {
        return spdxDocument;
    }

    public SpdxDocumentRoot getSpdxDocumentRoot() {
        return spdxDocumentRoot;
    }

    public Map<String, SpdxPackageEntity> getPackageLookupMap() {
        return packageLookupMap;
    }

    public void setPackageLookupMap(Map<String, SpdxPackageEntity> packageLookupMap) {
        this.packageLookupMap = packageLookupMap;
    }

    public Collection<ExtractedLicenseInfo> getExtractedLicenseInfos() {
        return extractedLicenseInfos;
    }

    public void setExtractedLicenseInfos(Collection<ExtractedLicenseInfo> extractedLicenseInfos) {
        this.extractedLicenseInfos = extractedLicenseInfos;
    }

    public Map<String, SoftwareComponent> getComponentCache() {
        return componentCache;
    }

    public void setComponentCache(Map<String, SoftwareComponent> componentCache) {
        this.componentCache = componentCache;
    }

    public Map<String, TemplateLicense> getLicenseCache() {
        return licenseCache;
    }

    public void setLicenseCache(Map<String, TemplateLicense> licenseCache) {
        this.licenseCache = licenseCache;
    }

    public Map<String, InventoryItem> getFileToInventoryItemMap() {
        return fileToInventoryItemMap;
    }

    public void setFileToInventoryItemMap(Map<String, InventoryItem> fileToInventoryItemMap) {
        this.fileToInventoryItemMap = fileToInventoryItemMap;
    }

    public Map<String, InventoryItem> getInventoryCache() {
        return inventoryCache;
    }

    public void setInventoryCache(Map<String, InventoryItem> inventoryCache) {
        this.inventoryCache = inventoryCache;
    }

    public List<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public Set<Long> getMainInventoryItems() {
        return mainInventoryItems;
    }

    public void setMainInventoryItems(Set<Long> mainInventoryItems) {
        this.mainInventoryItems = mainInventoryItems;
    }

    public Set<String> getMainPackageIds() {
        return mainPackageIds;
    }

    public void setMainPackageIds(Set<String> mainPackageIds) {
        this.mainPackageIds = mainPackageIds;
    }

    public Set<String> getProcessedFileIds() {
        return processedFileIds;
    }

    public void setProcessedFileIds(Set<String> processedFileIds) {
        this.processedFileIds = processedFileIds;
    }

    public Map<String, List<Relationship>> getPackageRelationships() {
        return packageRelationships;
    }

    public void setPackageRelationships(Map<String, List<Relationship>> packageRelationships) {
        this.packageRelationships = packageRelationships;
    }
}
