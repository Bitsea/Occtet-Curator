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

package eu.occtet.boc.spdx.service;

import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class PackageHandler {

    private static final Logger log = LogManager.getLogger(PackageHandler.class);

    @Autowired
    SpdxConverter spdxConverter;
    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private CopyrightRepository copyrightRepository;


    public InventoryItem parsePackages(SpdxPackage spdxPackage, Project project,
                                        SpdxDocumentRoot spdxDocumentRoot,
                                        Map<String, SpdxPackageEntity> packageLookupMap,
                                        Map<String, SoftwareComponent> componentCache,
                                        Map<String, License> licenseCache, Set<Long> mainInvetoryItems,
                                        Set<String> mainPackageIds,
                                        Map<String, InventoryItem> fileToInventoryItemMap,
                                        Set<String> processedFileIds,
                                       Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc)
            throws Exception {

        log.info("Looking at package: {}", spdxPackage.getId());
        //Convert to entities
        spdxConverter.convertPackage(spdxPackage, spdxDocumentRoot, packageLookupMap);

        String packageName = spdxPackage.getName().orElse(spdxPackage.getId());
        String version = spdxPackage.getVersionInfo().orElse("");
        List<Copyright> copyrights = new ArrayList<>();

        String componentKey = packageName + ":" + version;
        SoftwareComponent component = componentCache.get(componentKey);

        if (component == null) {
            // Cache Miss: Hit the DB
            component = softwareComponentService.getOrCreateSoftwareComponent(packageName, version);
            // Cache Put
            componentCache.put(componentKey, component);
        }

        //get License from package
        AnyLicenseInfo spdxPkgLicense = spdxPackage.getLicenseConcluded();
        if (spdxPkgLicense.isNoAssertion(spdxPkgLicense)) {
            spdxPkgLicense = spdxPackage.getLicenseDeclared();
        }

        List<License> pkgLicenses = licenseHandler.createLicenses(spdxPkgLicense, licenseCache, licenseInfosExtractedSpdxDoc);
        if(component.getLicenses() != null) {
            //make double sure there are no doubles
            Set<License> lSet= new HashSet<>( component.getLicenses());
            component.setLicenses(new ArrayList<>(lSet));
            SoftwareComponent finalComponent = component;
            pkgLicenses.stream()
                    .filter(license -> !finalComponent.getLicenses().contains(license))
                    .forEach(component::addLicense);
        } else {
            component.setLicenses(pkgLicenses);
        }

        String packageLicenseString = spdxPkgLicense != null ? spdxPkgLicense.toString() : "";

        String inventoryName = spdxPackage.getId().replaceAll("(?i)^SPDXRef-[^-]+-[^-]+-", "");
        if (!inventoryName.contains(component.getVersion())) inventoryName += component.getVersion();
        inventoryName += " (" + packageLicenseString + ")";

        //Check if license of component is combined
        Pattern pattern = Pattern.compile("\\b(?:BUT|AND)\\b");
        boolean isCombined = pattern.matcher(packageLicenseString).find();

        InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(inventoryName, component, project);
        inventoryItem.setWasCombined(isCombined);
        inventoryItem.setSpdxId(spdxPackage.getId());
        inventoryItem.setCurated(false);

        inventoryItem.setSize(spdxPackage.getFiles().size());

        log.info("Converting {} files", spdxPackage.getFiles().size());
        spdxPackage.getFiles().forEach(f -> {
            spdxConverter.convertFile(f, spdxDocumentRoot);
            fileToInventoryItemMap.put(f.getId(), inventoryItem);
            processedFileIds.add(f.getId());
        });

        try {
            copyrights = parseFiles(spdxPackage, inventoryItem);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error batch processing files", e);
        }


        if (component.getCopyrights() == null){
            component.setCopyrights(new ArrayList<>(copyrights)); // Copy to new list
        } else {
            Set<Copyright> uniqueCopyrights = new HashSet<>(component.getCopyrights());
            uniqueCopyrights.addAll(copyrights);
            component.setCopyrights(new ArrayList<>(uniqueCopyrights));
        }


        String downloadLocation = spdxPackage.getDownloadLocation().orElse("");
        component.setDetailsUrl(downloadLocation);

        List<ExternalRef> externalRefs = spdxPackage.getExternalRefs().stream().toList();
        for(ExternalRef externalRef: externalRefs){
            if(externalRef.getReferenceType().getIndividualURI().endsWith("purl")){
                component.setPurl(externalRef.getReferenceLocator());
                log.info("Found purl: {} for Component: {}", externalRef.getReferenceLocator(), component.getName());
            }
        }

        inventoryItemService.update(inventoryItem);
        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

        if (mainPackageIds.contains(spdxPackage.getId())) {
            mainInvetoryItems.add(inventoryItem.getId());
        }

        return inventoryItem;
    }

    private List<Copyright> parseFiles(SpdxPackage spdxPackage, InventoryItem inventoryItem) throws InvalidSPDXAnalysisException {
        List<String> allFileNames = new ArrayList<>();
        Set<String> allCopyrightsTexts = new HashSet<>();
        Map<String, String> fileToCopyrightMap = new HashMap<>();
        for (SpdxFile f : spdxPackage.getFiles()) {
            if (f.getName().isPresent()){
                String path = f.getName().get();
                allFileNames.add(path);
                String copyright = f.getCopyrightText();
                if (!"NONE".equals(copyright) && !"NOASSERTION".equals(copyright)){
                    allCopyrightsTexts.add(copyright);
                    fileToCopyrightMap.put(path, copyright);
                }
            }
        }


        Map<String, File> locationMap = fileService.findOrCreateBatch(allFileNames, inventoryItem);
        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts);

        List<Copyright> copyrightsToUpdate = new ArrayList<>();
        for (Map.Entry<String, String> entry : fileToCopyrightMap.entrySet()) {
            String path = entry.getKey();
            String copyrightText = entry.getValue();
            File loc = locationMap.get(path);
            Copyright copyright = copyrightMap.get(copyrightText);
            if (loc != null && copyright != null) {
                log.debug("Associating copyright '{}' with file '{}'", copyrightText, path);
                copyright.getFiles().add(loc);
                copyrightsToUpdate.add(copyright);
            }
        }
        copyrightRepository.saveAll(copyrightsToUpdate);

        return new ArrayList<>(copyrightMap.values());
    }
}
