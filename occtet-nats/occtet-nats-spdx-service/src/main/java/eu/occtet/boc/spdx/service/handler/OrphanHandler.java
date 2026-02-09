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

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.TypedValue;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class OrphanHandler {

    private static final Logger log = LogManager.getLogger(OrphanHandler.class);
    @Autowired
    private SpdxConverter spdxConverter;
    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private InventoryItemService inventoryItemService;



    public void processOrphanFiles(SpdxDocument spdxDocument, Project project,
                                    SpdxDocumentRoot spdxDocumentRoot,
                                    Map<String, License> licenseCache,
                                    Map<String, InventoryItem> fileToInventoryItemMap,
                                    Set<String> processedFileIds,
                                    List<InventoryItem> inventoryItems,
                                   Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc) throws Exception {

        List<TypedValue> allFileUris = spdxDocument.getModelStore().getAllItems(null, "File").toList();
        List<SpdxFile> orphanFiles = new ArrayList<>();

        for (TypedValue uri : allFileUris) {
            SpdxModelFactory.getSpdxObjects(
                    spdxDocument.getModelStore(),
                    spdxDocument.getCopyManager(),
                    "File",
                    uri.getObjectUri(),
                    null
            ).forEach(obj -> {
                if (obj instanceof SpdxFile file) {
                    if (!processedFileIds.contains(file.getId())) {
                        orphanFiles.add(file);
                    }
                }
            });
        }

        if (orphanFiles.isEmpty()) {
            return;
        }

        log.info("Found {} orphan files. Creating individual inventory items for each.", orphanFiles.size());


        for (SpdxFile file : orphanFiles) {

            String filePath = file.getName().orElse("Unknown File");

            SoftwareComponent component = softwareComponentService.getOrCreateSoftwareComponent(filePath, "Standalone");


            InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(filePath, component, project);
            inventoryItem.setSpdxId(file.getId());
            inventoryItem.setCurated(false);
            inventoryItem.setSize(1);


            spdxConverter.convertFile(file, spdxDocumentRoot);
            fileToInventoryItemMap.put(file.getId(), inventoryItem);

            Map<String, File> locationMap = fileService.findOrCreateBatch(Collections.singletonList(filePath), inventoryItem);
            File dbFile = locationMap.get(filePath);

            String copyrightText = file.getCopyrightText();
            if (copyrightText != null && !"NONE".equals(copyrightText) && !"NOASSERTION".equals(copyrightText)) {

                Map<String, Copyright> createdCopyrights = copyrightService.findOrCreateBatch(Collections.singleton(copyrightText));
                Copyright copyright = createdCopyrights.get(copyrightText);

                if (copyright != null) {
                    if (dbFile != null) {
                        copyright.getFiles().add(dbFile);
                        copyrightRepository.save(copyright);
                    }

                    if (component.getCopyrights() == null) {
                        component.setCopyrights(new ArrayList<>());
                    }
                    if (!component.getCopyrights().contains(copyright)) {
                        component.getCopyrights().add(copyright);
                    }
                }
            }

            AnyLicenseInfo fileLicense = file.getLicenseConcluded();
            if (fileLicense.isNoAssertion(fileLicense)) {
                fileLicense = file.getLicenseInfoFromFiles().stream().findFirst().orElse(null);
            }

            if (fileLicense != null) {
                List<License> licenses = licenseHandler.createLicenses(fileLicense, licenseCache, licenseInfosExtractedSpdxDoc);

                if (component.getLicenses() == null) {
                    component.setLicenses(new ArrayList<>());
                }

                for (License l : licenses) {
                    if (!component.getLicenses().contains(l)) {
                        component.addLicense(l);
                    }
                }
            }

            softwareComponentService.update(component);
            inventoryItemService.update(inventoryItem);
            inventoryItems.add(inventoryItem);
        }
    }
}
