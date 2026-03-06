/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.boc.export.service;

import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This service merges the changes contained in the <b>auditing entities</b> to its twin <b>document entities</b>
 */
@Service
public class MergeService {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    private SpdxFileSyncService spdxFileSyncService;

    /**
     * Traverses the components within an SPDX document and applies the corresponding audited data.
     * <p>It synchronizes changes applied but the auditor<p/>
     * Note: compatible with spdx version 2 entities
     */
    @Transactional
    public void mergeChangesToDocumentEntities(SpdxDocumentRoot spdxDocumentRoot, Project project) {
        log.info("Starting to merge curated changes for project: {}", project.getProjectName());
        Set<License> customLicensesToExtract = new HashSet<>();

        spdxDocumentRoot.setName(project.getProjectName());

        for (SpdxPackageEntity spdxPackageEntity : spdxDocumentRoot.getPackages()) {
            try {
                InventoryItem inventoryItem = inventoryItemRepository.findBySpdxIdAndProject(spdxPackageEntity.getSpdxId(), project).getFirst();
                SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();

                handleSpdxPackageEntity(spdxPackageEntity, inventoryItem, softwareComponent);

                if (softwareComponent != null && softwareComponent.getLicenses() != null) {
                    customLicensesToExtract.addAll(softwareComponent.getLicenses());
                }
            } catch (NoSuchElementException e) {
                log.debug("No inventoryItem was found for this package: {}, skip merging changes to document entities.", spdxPackageEntity.getSpdxId());
            } catch (Exception e) {
                log.warn("Unexpected error while resolving auditing entities for package: {}", spdxPackageEntity.getSpdxId(), e);
            }
        }
        handleExtractedLicenses(spdxDocumentRoot, customLicensesToExtract);

        spdxDocumentRootRepository.save(spdxDocumentRoot);
        log.info("Successfully merged curated changes for project: {}", project.getProjectName());
    }

    /**
     * Updates an SPDX package entity using the curated data from its associated inventory item and software component.
     * <p>
     * The update is conditionally applied only if the underlying component or inventory item
     * has been flagged as curated. It synchronizes metadata, resolves aggregated copyright
     * and license expressions, and rebuilds external references (such as PURLs and vulnerabilities).
     *
     * @param spdxPackageEntity The target document entity representing the package.
     * @param inventoryItem     The structural node defining the package's place in the audited tree.
     * @param softwareComponent The library metadata containing versioning, licensing, and vulnerability data.
     */
    private void handleSpdxPackageEntity(SpdxPackageEntity spdxPackageEntity, InventoryItem inventoryItem,
                                   SoftwareComponent softwareComponent) {
        boolean isCurated = Boolean.TRUE.equals(inventoryItem.getCurated()) || Boolean.TRUE.equals(softwareComponent.getCurated());

        if (!isCurated) {
            return;
        }

        spdxPackageEntity.setName(softwareComponent.getName());
        spdxPackageEntity.setVersionInfo(softwareComponent.getVersion());

        if (softwareComponent.getDetailsUrl() != null && !softwareComponent.getDetailsUrl().isBlank()) {
            // fix once decided
//            spdxPackageEntity.setHomepage(softwareComponent.getDetailsUrl());
            spdxPackageEntity.setDownloadLocation(softwareComponent.getDetailsUrl());
        }

        if (inventoryItem.getExternalNotes() != null && !inventoryItem.getExternalNotes().isBlank()) {
            spdxPackageEntity.setComment(inventoryItem.getExternalNotes());
        }

        if (softwareComponent.getCopyrights() != null) {
            String aggregatedCopyrights = aggregateValidCopyrights(softwareComponent.getCopyrights());
            if (!aggregatedCopyrights.isBlank()) {
                spdxPackageEntity.setCopyrightText(aggregatedCopyrights);
            }
        }

        if (softwareComponent.getLicenses() != null && !softwareComponent.getLicenses().isEmpty()) {
            String concludedLicenses = formatLicenseExpression(softwareComponent.getLicenses());
            spdxPackageEntity.setLicenseConcluded(concludedLicenses);
        }

        rebuildExternalReferences(spdxPackageEntity, softwareComponent);

        SpdxDocumentRoot spdxDocumentRoot = spdxPackageEntity.getSpdxDocument();
        spdxFileSyncService.synchronizeFiles(spdxPackageEntity, inventoryItem, spdxDocumentRoot);
    }

    /**
     * Clears and rebuilds external references for a package to ensure accurate PURL and vulnerability mapping.
     *
     * @param spdxPackageEntity The target document entity receiving the external references.
     * @param softwareComponent The source of the PURL and vulnerability data.
     */
    private void rebuildExternalReferences(SpdxPackageEntity spdxPackageEntity, SoftwareComponent softwareComponent) {
        List<ExternalRefEntity> externalRefs = new ArrayList<>();

        if (softwareComponent.getPurl() != null && !softwareComponent.getPurl().isBlank()) {
            ExternalRefEntity purlRef = new ExternalRefEntity();
            // FIX: Use underscore for the Java enum representation
            purlRef.setReferenceCategory("PACKAGE_MANAGER");
            purlRef.setReferenceType("purl");
            purlRef.setReferenceLocator(softwareComponent.getPurl());
            purlRef.setPkg(spdxPackageEntity);
            externalRefs.add(purlRef);
        }

        spdxPackageEntity.getExternalRefs().clear();
        spdxPackageEntity.getExternalRefs().addAll(externalRefs);
    }

     // Reconstructs the structural relationships based on the current inventory item hierarchy.
//    private void rebuildRelationships(SpdxDocumentRoot spdxDocumentRoot, Project project) {
//        // Discuss how relations between packages should be rebuilt
//    }

    /**
     * Converts non-standard or modified licenses into extracted licensing info for the SPDX document.
     *
     * @param spdxDocumentRoot The root document entity.
     * @param collectedLicenses The aggregate set of all licenses utilized across components and files.
     */
    private void handleExtractedLicenses(SpdxDocumentRoot spdxDocumentRoot, Set<License> collectedLicenses) {
        List<ExtractedLicensingInfoEntity> newExtractedLicenses = new ArrayList<>();

        for (License license : collectedLicenses) {
            if (license.getLicenseName() != null && license.getLicenseName().trim().equalsIgnoreCase("UNKNOWN")) {
                continue;
            }

            if (Boolean.FALSE.equals(license.isSpdx()) || Boolean.TRUE.equals(license.isModified())) {
                ExtractedLicensingInfoEntity extractedInfo = new ExtractedLicensingInfoEntity();
                extractedInfo.setSpdxDocument(spdxDocumentRoot);
                extractedInfo.setLicenseId(generateLicenseRefId(license));
                extractedInfo.setName(license.getLicenseName() != null ? license.getLicenseName() : "Custom License");
                extractedInfo.setExtractedText(license.getLicenseText());
                newExtractedLicenses.add(extractedInfo);
            }
        }

        for (ExtractedLicensingInfoEntity newEntity : newExtractedLicenses) {
            boolean exists = spdxDocumentRoot.getHasExtractedLicensingInfos().stream().anyMatch(existing -> existing.getLicenseId().equals(newEntity.getLicenseId()));
            if (!exists) {
                spdxDocumentRoot.getHasExtractedLicensingInfos().add(newEntity);
            }
        }
    }

    /**
     * Formats a collection of valid copyrights into a unified text block.
     *
     * @param copyrights The raw set of copyrights to filter and format.
     * @return A consolidated string containing all valid copyright texts.
     */
    private String aggregateValidCopyrights(Collection<Copyright> copyrights) {
        return copyrights.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getGarbage()))
                .map(Copyright::getCopyrightText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Constructs a valid SPDX license expression string from a list of licenses.
     *
     * @param licenses The list of applied licenses.
     * @return The formatted license expression string (e.g., "MIT AND LicenseRef-12").
     */
    private String formatLicenseExpression(Collection<License> licenses) {
        return licenses.stream()
                .map(license -> {
                    String name = license.getLicenseName();
                    // Intercept UNKNOWN and replace with SPDX standard NOASSERTION
                    if (name != null && name.trim().equalsIgnoreCase("UNKNOWN")) {
                        return "NOASSERTION";
                    }
                    if (Boolean.FALSE.equals(license.isSpdx()) || Boolean.TRUE.equals(license.isModified())) {
                        return generateLicenseRefId(license);
                    }
                    return name;
                })
                .collect(Collectors.joining(" AND "));
    }

    private String generateLicenseRefId(License license) {
        if (license.getLicenseName() != null && !license.getLicenseName().isBlank()) {
            String sanitizedName = license.getLicenseName().replaceAll("[^A-Za-z0-9.-]", "-");
            sanitizedName = sanitizedName.replaceAll("-+", "-");
            return "LicenseRef-" + sanitizedName;
        }
        return "LicenseRef-custom-" + license.getId();
    }

}
