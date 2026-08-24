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

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.CopyrightService;
import eu.occtet.boc.spdx.service.FileService;
import eu.occtet.boc.spdx.service.InventoryItemService;
import eu.occtet.boc.spdx.service.SoftwareComponentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.TypedValue;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
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
    @Autowired
    private OrtIssueRepository ortIssueRepository;
    @Autowired
    private OrtViolationRepository ortViolationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public void processAllPackages(SpdxImportContext context, Consumer<Integer> progressCallback) {
        SpdxDocument doc = context.getSpdxDocument();
        if (doc == null || doc.getModelStore() == null) {
            log.warn("Model store is empty, skipping package processing.");
            return;
        }
        try {
            List<TypedValue> packageUris = doc.getModelStore().getAllItems(null, "Package").toList();

            int count = 0;
            Set<String> seenPackages = new HashSet<>();
            Set<InventoryItem> inventoryItemsToSave = new HashSet<>();
            Set<Copyright> copyrightsToSave = new HashSet<>();

            for (TypedValue uri : packageUris) {
                try {
                    SpdxModelFactory.getSpdxObjects(doc.getModelStore(), null, "Package", uri.getObjectUri(), null)
                            .forEach(obj -> {
                                if (obj instanceof SpdxPackage pkg && !seenPackages.contains(pkg.getId())) {
                                    log.debug("Processing package: {} (URI: {})", pkg.getId(), uri.getObjectUri());

                                    // skip -vcs and -source-artifact packages, as they are handled in parseSinglePackage
                                    // the files from these packages are merged into the main package's files, so we don't need to create separate inventory items for them
                                    if (pkg.getId().endsWith("-source-artifact") || pkg.getId().endsWith("-vcs")) {
                                        seenPackages.add(pkg.getId());
                                        return;
                                    }

                                    try {
                                        InventoryItem item = parseSinglePackage(pkg, context, copyrightsToSave);
                                        context.getInventoryItems().add(item);
                                        inventoryItemsToSave.add(item);
                                        seenPackages.add(pkg.getId());

                                        List<Relationship> relationships = pkg.getRelationships().stream().toList();
                                        context.getPackageRelationships().put(pkg.getId(), relationships);
                                        log.debug("Stored {} relationships for package {}", relationships.size(), pkg.getId());
                                    } catch (Exception e) {
                                        log.error("Failed to import package {}: {}. Skipping...", pkg.getId(),
                                                e.getMessage());
                                    }
                                }
                            });
                } catch (Exception e) {
                    log.error("Error retrieving SPDX object for URI: {}", uri.getObjectUri(), e);
                }

                count++;
                int percent = (int) ((40.0 * count) / packageUris.size());
                if (percent % 5 == 0) progressCallback.accept(percent);
            }

            projectRepository.save(context.getProject());
            if (!copyrightsToSave.isEmpty()) {
                copyrightRepository.saveAll(copyrightsToSave);
            }
            if (!inventoryItemsToSave.isEmpty()) {
                inventoryItemRepository.saveAll(inventoryItemsToSave);
            }

        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error retrieving SPDX object for URI: {}", e.getMessage(), e);
        }
    }

    public InventoryItem parseSinglePackage(SpdxPackage spdxPackage, SpdxImportContext context,
            Set<Copyright> copyrightsToSave)
            throws Exception {

        List<OrtIssue> ortIssues = ortIssueRepository.findByProject(context.getProject());
        List<OrtViolation> ortViolations = ortViolationRepository.findByProject(context.getProject());

        log.info("Looking at package: {}", spdxPackage.getId());
        spdxConverter.convertPackage(spdxPackage, context.getSpdxDocumentRoot(), context.getPackageLookupMap());

        String packageName = spdxPackage.getName().orElse(spdxPackage.getId());
        String version = spdxPackage.getVersionInfo().orElse("");
        List<Copyright> copyrights = new ArrayList<>();

        String componentKey = packageName + ":" + version;
        SoftwareComponent component = context.getComponentCache().get(componentKey);

        if (component == null) {
            component = softwareComponentService.getOrCreateSoftwareComponent(packageName, version,
                    context.getProject().getOrganization(), "library");
            context.getComponentCache().put(componentKey, component);
        }

        // get License from package
        AnyLicenseInfo spdxPkgLicense = spdxPackage.getLicenseConcluded();
        if (spdxPkgLicense == null || spdxPkgLicense.isNoAssertion(spdxPkgLicense)) {
            spdxPkgLicense = spdxPackage.getLicenseDeclared();
        }

        licenseHandler.createUsageLicenses(spdxPkgLicense, context,
                context.getExtractedLicenseInfos(), component, context.getProject().getOrganization());

        String packageLicenseString = spdxPkgLicense != null ? spdxPkgLicense.toString() : "";

        String inventoryName = spdxPackage.getId().replaceAll("(?i)^SPDXRef-[^-]+-[^-]+-", "");
        if (!inventoryName.contains(component.getVersion()))
            inventoryName += component.getVersion();
        inventoryName += " (" + packageLicenseString + ")";

        InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(inventoryName, component,
                context.getProject(),
                context.getProject().getOrganization());
        inventoryItem.setSpdxId(spdxPackage.getId());
        inventoryItem.setCurated(false);

        inventoryItemService.sortViolationsAndIssues(ortIssues, ortViolations, inventoryItem);

        Set<SpdxFile> packageFiles = new HashSet<>(spdxPackage.getFiles());

        try {
            // 1. direct contains data from main package
            spdxPackage.getRelationships().stream()
                    .filter(this::isContainsRelationship)
                    .map(this::getRelatedElementOrNull)
                    .filter(element -> element instanceof SpdxFile)
                    .map(SpdxFile.class::cast)
                    .forEach(packageFiles::add);

            // 2. generated packages (e.g., -vcs, -source-artifact) that contain files
            spdxPackage.getRelationships().stream()
                    .filter(r -> {
                        try {
                            return r.getRelationshipType() == RelationshipType.GENERATED_FROM;
                        } catch (InvalidSPDXAnalysisException e) {
                            return false;
                        }
                    })
                    .map(this::getRelatedElementOrNull)
                    .filter(element -> element instanceof SpdxPackage)
                    .map(SpdxPackage.class::cast)
                    .forEach(relatedPkg -> {
                        try {
                            // data directly from the related package
                            packageFiles.addAll(relatedPkg.getFiles());

                            // data from the related package's CONTAINS relationships
                            relatedPkg.getRelationships().stream()
                                    .filter(this::isContainsRelationship)
                                    .map(this::getRelatedElementOrNull)
                                    .filter(e -> e instanceof SpdxFile)
                                    .map(SpdxFile.class::cast)
                                    .forEach(packageFiles::add);

                            log.debug("Merged source files from related package: {}", relatedPkg.getId());
                        } catch (Exception e) {
                            log.warn("Error resolving files from related source package {}", relatedPkg.getId(), e);
                        }
                    });

        } catch (Exception e) {
            log.warn("Error resolving relationships for package {}", spdxPackage.getId(), e);
        }

        inventoryItem.setSize(packageFiles.size());
        log.info("Converting {} files ({} explicit, {} total)",
                spdxPackage.getFiles().size(), spdxPackage.getFiles().size(), packageFiles.size());

        packageFiles.forEach(f -> {
            spdxConverter.convertFile(f, context.getSpdxDocumentRoot());
            context.getFileToInventoryItemMap().put(f.getId(), inventoryItem);
            context.getProcessedFileIds().add(f.getId());
        });

        try {
            copyrights = parseFiles(packageFiles, inventoryItem, context, copyrightsToSave);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error batch processing files", e);
        }

        if (component.getCopyrights() == null) {
            component.setCopyrights(new ArrayList<>(copyrights)); // Copy to new list
        } else {
            Set<Copyright> uniqueCopyrights = new HashSet<>(component.getCopyrights());
            uniqueCopyrights.addAll(copyrights);
            component.setCopyrights(new ArrayList<>(uniqueCopyrights));
        }

        String downloadLocation = spdxPackage.getDownloadLocation().orElse("");
        if (!isValidDownloadLocation(downloadLocation)) {
            downloadLocation = findRelatedDownloadLocation(spdxPackage);
        }
        if (isValidDownloadLocation(downloadLocation)) {
            component.setDetailsUrl(downloadLocation);
        } else {
            component.setDetailsUrl(null);
        }


        List<ExternalRef> externalRefs = spdxPackage.getExternalRefs().stream().toList();
        for (ExternalRef externalRef : externalRefs) {
            if (externalRef.getReferenceType().getIndividualURI().endsWith("purl")) {
                component.setPurl(externalRef.getReferenceLocator());
                log.info("Found purl: {} for Component: {}", externalRef.getReferenceLocator(), component.getName());
            }
        }

        log.info("created inventoryItem: {}", inventoryName);
        log.info("created softwareComponent: {}", component.getName());

        if (context.getMainPackageIds().contains(spdxPackage.getId())) {
            context.getMainInventoryItems().add(inventoryItem.getId());
        }

        return inventoryItem;
    }

    private List<Copyright> parseFiles(Set<SpdxFile> packageFiles, InventoryItem inventoryItem,
            SpdxImportContext context, Set<Copyright> copyrightsToSave) throws InvalidSPDXAnalysisException {
        Set<String> allCopyrightsTexts = new HashSet<>();
        Map<String, String> fileToCopyrightMap = new HashMap<>();
        Map<String, String> fileToSpdxIdMap = new HashMap<>();

        for (SpdxFile f : packageFiles) {
            context.getProcessedFileIds().add(f.getId());
            if (f.getName().isPresent()) {
                String path = f.getName().get();
                fileToSpdxIdMap.put(path, f.getId());

                String copyright = f.getCopyrightText();
                if (!"NONE".equals(copyright) && !"NOASSERTION".equals(copyright)) {
                    allCopyrightsTexts.add(copyright);
                    fileToCopyrightMap.put(path, copyright);
                }
            }
        }

        Map<String, File> locationMap = fileService.findOrCreateBatch(fileToSpdxIdMap, inventoryItem);

        Project project = inventoryItem.getProject();
        project.addFiles(new HashSet<>(locationMap.values()));

        Map<String, Copyright> copyrightMap = copyrightService.findOrCreateBatch(allCopyrightsTexts,
                context.getProject().getOrganization());

        for (Map.Entry<String, String> entry : fileToCopyrightMap.entrySet()) {
            String path = entry.getKey();
            String copyrightText = entry.getValue();
            File loc = locationMap.get(path);
            Copyright copyright = copyrightMap.get(copyrightText);
            if (loc != null && copyright != null) {
                log.debug("Associating copyright '{}' with file '{}'", copyrightText, path);
                copyright.getFiles().add(loc);
                copyrightsToSave.add(copyright);
            }
        }

        return new ArrayList<>(copyrightMap.values());
    }

    private boolean isContainsRelationship(Relationship r) {
        try {
            return r.getRelationshipType() == RelationshipType.CONTAINS;
        } catch (InvalidSPDXAnalysisException e) {
            return false;
        }
    }

    private SpdxElement getRelatedElementOrNull(Relationship r) {
        try {
            return r.getRelatedSpdxElement().orElse(null);
        } catch (InvalidSPDXAnalysisException e) {
            return null;
        }
    }

    private String findRelatedDownloadLocation(SpdxPackage spdxPackage) {
        try {
            for (Relationship rel : spdxPackage.getRelationships()) {
                RelationshipType relType = rel.getRelationshipType();
                if (relType == RelationshipType.GENERATED_FROM || relType == RelationshipType.CONTAINS) {
                    SpdxElement relatedElement = rel.getRelatedSpdxElement().orElse(null);
                    if (relatedElement instanceof SpdxPackage relatedPkg) {
                        String loc = relatedPkg.getDownloadLocation().orElse("");
                        if (isValidDownloadLocation(loc)) {
                            log.info("Found downloadLocation from related package {} via {}: {}",
                                    relatedPkg.getId(), relType, loc);
                            return loc;
                        }
                    }
                }
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.warn("Error resolving relationships to find related downloadLocation for package {}",
                    spdxPackage.getId(), e);
        }
        return "";
    }

    private boolean isValidDownloadLocation(String location) {
        return location != null && !location.isBlank()
                && !"NONE".equalsIgnoreCase(location)
                && !"NOASSERTION".equalsIgnoreCase(location);
    }
}