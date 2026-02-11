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

package eu.occtet.boc.spdx.context;

import eu.occtet.boc.entity.InventoryItem;

import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import lombok.Data;
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
    private Map<String, License> licenseCache = new HashMap<>();
    private Map<String, InventoryItem> fileToInventoryItemMap = new HashMap<>();
    private Map<String, InventoryItem> inventoryCache = new HashMap<>();

    // Tracking Sets
    private List<InventoryItem> inventoryItems = new ArrayList<>();
    private Set<Long> mainInventoryItems = new HashSet<>();
    private Set<String> mainPackageIds = new HashSet<>();
    private Set<String> processedFileIds = new HashSet<>();

    public SpdxImportContext(Project project, SpdxDocument spdxDocument, SpdxDocumentRoot root) {
        this.project = project;
        this.spdxDocument = spdxDocument;
        this.spdxDocumentRoot = root;
    }
}
