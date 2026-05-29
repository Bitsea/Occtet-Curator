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

package eu.occtet.boc.cyclonedx.context;

import eu.occtet.boc.entity.*;

import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import lombok.Data;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;

import java.util.*;

@Data
public class CycloneDxImportContext {

    private final Project project;

    // Caches and State
    private Map<String, SoftwareComponent> componentCache = new HashMap<>();
    private Map<String, SoftwareComponent> componentVulnerabilityCache = new HashMap<>();

    private Map<String, License> licenseCache = new HashMap<>();
    //here we take the softwarecomponent name as key, because the same license can be used for multiple components, but a component can only have one usage license
    private Map<String, SoftwareComponentLicenseUsage> usageLicenseCache = new HashMap<>();
    private Map<String, InventoryItem> fileToInventoryItemMap = new HashMap<>();
    private Map<String, InventoryItem> inventoryCache = new HashMap<>();

    // Tracking Sets
    private List<InventoryItem> inventoryItems = new ArrayList<>();
    private Set<Long> mainInventoryItems = new HashSet<>();

    public CycloneDxImportContext(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }


    public Map<String, SoftwareComponent> getComponentCache() {
        return componentCache;
    }

    public void setComponentCache(Map<String, SoftwareComponent> componentCache) {
        this.componentCache = componentCache;
    }

    public Map<String, License> getLicenseCache() {
        return licenseCache;
    }

    public void setLicenseCache(Map<String, License> licenseCache) {
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


    public Map<String, SoftwareComponentLicenseUsage> getUsageLicenseCache() {
        return usageLicenseCache;
    }

    public void setUsageLicenseCache(Map<String, SoftwareComponentLicenseUsage> usageLicenseCache) {
        this.usageLicenseCache = usageLicenseCache;
    }

    public Map<String, SoftwareComponent> getComponentVulnerabilityCache() {
        return componentVulnerabilityCache;
    }

    public void setComponentVulnerabilityCache(Map<String, SoftwareComponent> componentVulnerabilityCache) {
        this.componentVulnerabilityCache = componentVulnerabilityCache;
    }
}
