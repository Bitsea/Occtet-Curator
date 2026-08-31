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

import java.net.URI;
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
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

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

            // Register the main item's DB ID now that all entities are persisted.
            // This must happen after saveAll() so that getId() is non-null.
            if (context.getMainItem() != null && context.getMainItem().getId() != null) {
                context.getMainInventoryItems().add(context.getMainItem().getId());
                log.debug("Registered main inventory item ID {} into context", context.getMainItem().getId());
            } else {
                log.warn("Main item could not be registered: item is null or has no DB ID yet");
            }
            if (!context.getComponentCache().isEmpty()) {
                softwareComponentRepository.saveAll(context.getComponentCache().values());
            }

        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error retrieving SPDX object for URI: {}", e.getMessage(), e);
        }
    }

    public InventoryItem parseSinglePackage(SpdxPackage spdxPackage, SpdxImportContext context,
                                            Set<Copyright> copyrightsToSave) throws Exception {

        log.info("Looking at package: {}", spdxPackage.getId());
        spdxConverter.convertPackage(spdxPackage, context.getSpdxDocumentRoot(), context.getPackageLookupMap());

        boolean isMainPackage = context.getMainPackageIds().contains(spdxPackage.getId());


        // component & licenses
        SoftwareComponent component = resolveSoftwareComponent(spdxPackage, context);
        AnyLicenseInfo spdxLicense = resolvePackageLicense(spdxPackage, context, component);

        // create or retrieve InventoryItem for this package
        InventoryItem inventoryItem = createAndConfigureInventoryItem(spdxPackage, component, spdxLicense, isMainPackage, context);

        // collect data
        Set<SpdxFile> packageFiles = collectAllPackageFiles(spdxPackage);
        processFilesAndCopyrights(packageFiles, inventoryItem, component, context, copyrightsToSave);

        // resolve dowload url, fallback to project repo url if main package and no valid download location found
        String downloadUrl = resolveDownloadLocation(spdxPackage, isMainPackage, context);
        component.setDetailsUrl(downloadUrl);

        // extract purl
        extractAndSetPurl(spdxPackage, component);

        log.info("Successfully processed inventoryItem: {} (isMain={})", inventoryItem.getInventoryName(), isMainPackage);
        return inventoryItem;
    }

    private String resolveDownloadLocation(SpdxPackage spdxPackage, boolean isMainPackage, SpdxImportContext context) {
       try {
           String location = spdxPackage.getDownloadLocation().orElse("");

           if (!isValidDownloadLocation(location)) {
               location = findRelatedDownloadLocation(spdxPackage);
           }

           // fallback repo-url
           if (!isValidDownloadLocation(location) && isMainPackage) {
               String projectRepoUrl = context.getProject().getRepositoryURL();
               if (isValidDownloadLocation(projectRepoUrl)) {
                   location = projectRepoUrl;
                   log.info("Using project repository URL '{}' as fallback for main package {}", location, spdxPackage.getId());
               }
           }

           return isValidDownloadLocation(location) ? location : null;
       }catch (Exception e) {
           log.warn("Error resolving download location for package {}: {}", spdxPackage.getId(), e.getMessage());
           return null;
       }
    }


    private InventoryItem createAndConfigureInventoryItem(SpdxPackage spdxPackage, SoftwareComponent component,
                                                          AnyLicenseInfo spdxPkgLicense, boolean isMainPackage, SpdxImportContext context) {

        String packageLicenseString = spdxPkgLicense != null ? spdxPkgLicense.toString() : "";
        String inventoryName = spdxPackage.getId().replaceAll("(?i)^SPDXRef-[^-]+-[^-]+-", "");
        if (!inventoryName.contains(component.getVersion())) {
            inventoryName += component.getVersion();
        }
        inventoryName += " (" + packageLicenseString + ")";

        InventoryItem inventoryItem = inventoryItemService.getOrCreateInventoryItem(
                inventoryName, component, context.getProject(), context.getProject().getOrganization());

        inventoryItem.setSpdxId(spdxPackage.getId());
        inventoryItem.setCurated(false);


        if (isMainPackage) {
            // Note: inventoryItem.getId() may still be null here (entity not yet persisted).
            // The ID is registered into context.mainInventoryItems after saveAll() in processAllPackages().
            context.setMainItem(inventoryItem);
            if(!inventoryItem.getSoftwareComponent().getVersion().equals(context.getProject().getVersion())){
                inventoryItem.getSoftwareComponent().setVersion(context.getProject().getVersion());
            }
        }else {
            inventoryItem.setParent(context.getMainItem());
            context.getMainItem().getDependencies().add(inventoryItem);
        }

        List<OrtIssue> ortIssues = ortIssueRepository.findByProject(context.getProject());
        List<OrtViolation> ortViolations = ortViolationRepository.findByProject(context.getProject());
        inventoryItemService.sortViolationsAndIssues(ortIssues, ortViolations, inventoryItem);

        return inventoryItem;
    }

    private Set<SpdxFile> collectAllPackageFiles(SpdxPackage spdxPackage) {
        try {
        Set<SpdxFile> packageFiles = new HashSet<>(spdxPackage.getFiles());


            // Direct CONTAINS files
            spdxPackage.getRelationships().stream()
                    .filter(this::isContainsRelationship)
                    .map(this::getRelatedElementOrNull)
                    .filter(SpdxFile.class::isInstance)
                    .map(SpdxFile.class::cast)
                    .forEach(packageFiles::add);

            // Files via GENERATED_FROM (-vcs, -source-artifact)
            spdxPackage.getRelationships().stream()
                    .filter(r -> {
                        try {
                            return r.getRelationshipType() == RelationshipType.GENERATED_FROM;
                        } catch (InvalidSPDXAnalysisException e) {
                            return false;
                        }
                    })
                    .map(this::getRelatedElementOrNull)
                    .filter(SpdxPackage.class::isInstance)
                    .map(SpdxPackage.class::cast)
                    .forEach(relatedPkg -> {
                        try {
                            packageFiles.addAll(relatedPkg.getFiles());
                            relatedPkg.getRelationships().stream()
                                    .filter(this::isContainsRelationship)
                                    .map(this::getRelatedElementOrNull)
                                    .filter(SpdxFile.class::isInstance)
                                    .map(SpdxFile.class::cast)
                                    .forEach(packageFiles::add);
                        } catch (Exception e) {
                            log.warn("Error resolving files from related package {}", relatedPkg.getId(), e);
                        }
                    });
            return packageFiles;
        } catch (Exception e) {
            log.warn("Error resolving relationships for package {}", spdxPackage.getId(), e);
            return new HashSet<>();
        }


    }

    private SoftwareComponent resolveSoftwareComponent(SpdxPackage spdxPackage, SpdxImportContext context) throws Exception {
        String packageName = spdxPackage.getName().orElse(spdxPackage.getId());
        String version = spdxPackage.getVersionInfo().orElse("");
        String componentKey = packageName + ":" + version;

        SoftwareComponent component = context.getComponentCache().get(componentKey);
        if (component == null) {
            component = softwareComponentService.getOrCreateSoftwareComponent(
                    packageName, version, context.getProject().getOrganization(), "library");
            context.getComponentCache().put(componentKey, component);
        }
        return component;
    }

    private AnyLicenseInfo resolvePackageLicense(SpdxPackage spdxPackage, SpdxImportContext context, SoftwareComponent component) throws Exception {
        AnyLicenseInfo spdxLicense = spdxPackage.getLicenseConcluded();
        if (spdxLicense == null || spdxLicense.isNoAssertion(spdxLicense)) {
            spdxLicense = spdxPackage.getLicenseDeclared();
        }
        licenseHandler.createUsageLicenses(spdxLicense, context,
                context.getExtractedLicenseInfos(), component, context.getProject().getOrganization());
        return spdxLicense;
    }

    private void processFilesAndCopyrights(Set<SpdxFile> packageFiles, InventoryItem inventoryItem,
                                           SoftwareComponent component, SpdxImportContext context, Set<Copyright> copyrightsToSave) {

        inventoryItem.setSize(packageFiles.size());
        log.info("Converting {} total files for item {}", packageFiles.size(), inventoryItem.getInventoryName());

        packageFiles.forEach(f -> {
            spdxConverter.convertFile(f, context.getSpdxDocumentRoot());
            context.getFileToInventoryItemMap().put(f.getId(), inventoryItem);
            context.getProcessedFileIds().add(f.getId());
        });

        List<Copyright> copyrights = new ArrayList<>();
        try {
            copyrights = parseFiles(packageFiles, inventoryItem, context, copyrightsToSave);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error batch processing files", e);
        }

        if (component.getCopyrights() == null) {
            component.setCopyrights(new ArrayList<>(copyrights));
        } else {
            Set<Copyright> uniqueCopyrights = new HashSet<>(component.getCopyrights());
            uniqueCopyrights.addAll(copyrights);
            component.setCopyrights(new ArrayList<>(uniqueCopyrights));
        }
    }

    private void extractAndSetPurl(SpdxPackage spdxPackage, SoftwareComponent component) {
        try {
            for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
                if (externalRef.getReferenceType().getIndividualURI().endsWith("purl")) {
                    component.setPurl(externalRef.getReferenceLocator());
                    log.info("Found purl: {} for Component: {}", externalRef.getReferenceLocator(), component.getName());
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process external refs for PURL: {}", e.getMessage());
        }
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
                //log.debug("Associating copyright '{}' with file '{}'", copyrightText, path);
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
        if (location == null || location.isBlank()) return false;

        String cleanLocation = location.trim();
        if ("NONE".equalsIgnoreCase(cleanLocation) || "NOASSERTION".equalsIgnoreCase(cleanLocation)) {
            return false;
        }

        // git+ temporarily removing
        if (cleanLocation.startsWith("git+")) {
            cleanLocation = cleanLocation.substring(4);
        }

        try {
            URI uri = new URI(cleanLocation);
            //  http, https, git, ssh
            return uri.getScheme() != null && !uri.getScheme().isBlank();
        } catch (Exception e) {
            log.warn("Invalid URL syntax found: {}", location);
            return false;
        }
    }
}