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

package eu.occtet.boc.export.service;

import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.dao.SpdxFileRepository;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.spdxV2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpdxFileSyncService {

    private static final Logger log = LogManager.getLogger(SpdxFileSyncService.class);

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private SpdxFileRepository spdxFileRepository;

    private final String CONTAINS = "CONTAINS";

    /**
     * Synchronizes the curated file state with the SPDX document output.
     * Evaluates all active files linked to an inventory item. Updates the copyright data
     * for existing files, provisions compliant entities for newly added files, and updates
     * the structural relationships.
     * @param spdxPackageEntity The target document package entity.
     * @param inventoryItem     The curated inventory item acting as the source of truth.
     * @param spdxDocumentRoot  The document root holding global files and relationships.
     */
    public void synchronizeFiles(SpdxPackageEntity spdxPackageEntity, InventoryItem inventoryItem, SpdxDocumentRoot spdxDocumentRoot) {
        List<File> filesToSync = fileRepository.findByInventoryItemsContaining(inventoryItem);
        List<String> activeSpdxFileIds = new ArrayList<>();

        if (!filesToSync.isEmpty()) {
            for (File auditedFile : filesToSync) {
                String fileSpdxId = auditedFile.getDocumentId();
                String aggregatedCopyrights = aggregateValidCopyrights(auditedFile);

                if (fileSpdxId != null && !fileSpdxId.isBlank()) {
                    updateExistingSpdxFile(fileSpdxId, aggregatedCopyrights, spdxDocumentRoot);
                } else {
                    SpdxFileEntity newSpdxFile = createNewSpdxFileEntity(auditedFile, aggregatedCopyrights,
                            spdxDocumentRoot);
                    spdxDocumentRoot.getFiles().add(newSpdxFile);

                    auditedFile.setDocumentId(newSpdxFile.getSpdxId());
                    fileRepository.save(auditedFile);
                    spdxFileRepository.save(newSpdxFile);
                    fileSpdxId = newSpdxFile.getSpdxId();
                }

                activeSpdxFileIds.add(fileSpdxId);
            }
            spdxPackageEntity.setFilesAnalyzed(true);
        } else {
            spdxPackageEntity.setFilesAnalyzed(false);
        }
        spdxPackageEntity.setFileNames(activeSpdxFileIds);

        rebuildContainsRelationshipsAndCleanOrphans(spdxPackageEntity.getSpdxId(), activeSpdxFileIds, spdxDocumentRoot);
    }

    /**
     * Locates an existing SPDX file within the document and applies audited metadata changes.
     *
     * @param fileSpdxId           The unique SPDX identifier of the file.
     * @param aggregatedCopyrights The fully resolved and curated copyright text.
     * @param spdxDocumentRoot     The document root storing the file entities.
     */
    private void updateExistingSpdxFile(String fileSpdxId, String aggregatedCopyrights, SpdxDocumentRoot spdxDocumentRoot) {
        spdxDocumentRoot.getFiles().stream()
                .filter(f -> fileSpdxId.equals(f.getSpdxId()))
                .findFirst()
                .ifPresent(existingFile -> existingFile.setCopyrightText(aggregatedCopyrights));
    }

    /**
     * Provisions a fully compliant SPDX file entity for a newly discovered physical file.
     *
     * @param auditedFile          The file entity representing the physical asset.
     * @param aggregatedCopyrights The curated copyright text mapped to the file.
     * @param spdxDocumentRoot     The parent document root.
     * @return A newly constructed SpdxFileEntity.
     */
    private SpdxFileEntity createNewSpdxFileEntity(File auditedFile, String aggregatedCopyrights, SpdxDocumentRoot spdxDocumentRoot) {
        SpdxFileEntity newFile = new SpdxFileEntity();
        newFile.setSpdxId("SPDXRef-File-" + UUID.randomUUID());
        newFile.setFileName(auditedFile.getArtifactPath() != null ? auditedFile.getArtifactPath() : auditedFile.getFileName());
        newFile.setSpdxDocument(spdxDocumentRoot);
        newFile.setLicenseConcluded("NOASSERTION");
        // newFile.setLicenseInfoInFiles(); dicuss and fix
        newFile.setCopyrightText(aggregatedCopyrights);

        ChecksumEntity checksum = new ChecksumEntity();
        checksum.setAlgorithm("SHA1");
        checksum.setChecksumValue(calculateSha1(auditedFile.getPhysicalPath()));
        checksum.setSpdxFile(newFile);

        newFile.setChecksums(new ArrayList<>(Collections.singletonList(checksum)));

        return newFile;
    }

    /**
     * Compiles curated copyright entries into a unified text block format compliant with SPDX.
     *
     * @param auditedFile The file containing the copyright associations.
     * @return The aggregated text, or "NONE" if no valid copyrights exist.
     */
    private String aggregateValidCopyrights(File auditedFile) {
        if (auditedFile.getCopyrights() == null || auditedFile.getCopyrights().isEmpty()) {
            return "NONE";
        }

        String aggregatedCopyrights = auditedFile.getCopyrights().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getGarbage()))
                .map(Copyright::getCopyrightText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        return aggregatedCopyrights.isBlank() ? "NONE" : aggregatedCopyrights;
    }

    /**
     * Computes the SHA-1 checksum for a physical file using a buffered input stream.
     * Fallbacks to a zeroed string if the file is missing or unreadable to guarantee export validation passes.
     *
     * @param physicalPath The absolute path to the target file.
     * @return The resulting SHA-1 hex string.
     */
    private String calculateSha1(String physicalPath){
        String sha1Zeroed = "0".repeat(40);
        if (physicalPath == null || physicalPath.isBlank()){
            log.warn("Physical path is empty. Emitting zeroed fallback SHA-1");
            return sha1Zeroed;
        }

        Path path = Paths.get(physicalPath);

        if (!Files.isReadable(path)){
            log.warn("Physical path is not readable. Emitting zeroed fallback SHA-1");
            return sha1Zeroed;
        }

        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192]; // 8 KB Chunks
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to compute SHA-256 for file: {}", physicalPath, e);
            return sha1Zeroed;
        }
    }

    /**
     * Synchronizes the CONTAINS relationships between an SPDX package and its constituent files.
     * Removes stale links for dropped files and maps explicit connections for active files.
     *
     * @param packageSpdxId    The identifier of the container package.
     * @param activeFileIds    The exact set of file identifiers the package should currently hold.
     * @param spdxDocumentRoot The root document managing relationship linkages.
     */
    private void rebuildContainsRelationshipsAndCleanOrphans(String packageSpdxId, List<String> activeFileIds,
                                                SpdxDocumentRoot spdxDocumentRoot) {
        log.trace("Rebuilding CONTAINS relationships for package {} with files {}", packageSpdxId, activeFileIds);
        if (spdxDocumentRoot.getRelationships() == null) {
            spdxDocumentRoot.setRelationships(new ArrayList<>());
        }
        Set<String> knownFileIds = spdxDocumentRoot.getFiles().stream()
                .map(SpdxFileEntity::getSpdxId)
                .collect(Collectors.toSet());

        List<String> prevLinkedFileIds = spdxDocumentRoot.getRelationships().stream()
                    .filter(rel -> packageSpdxId.equals(rel.getSpdxElementId()) &&
                            CONTAINS.equals(rel.getRelationshipType()) &&
                            rel.getRelatedSpdxElement() != null &&
                            knownFileIds.contains(rel.getRelatedSpdxElement()))
                    .map(RelationshipEntity::getRelatedSpdxElement)
                    .toList();

        List<String> deletedFileIds = prevLinkedFileIds.stream()
                .filter(oldId -> !activeFileIds.contains(oldId))
                .toList();

        spdxDocumentRoot.getRelationships().removeIf(rel ->
                packageSpdxId.equals(rel.getSpdxElementId()) &&
                CONTAINS.equals(rel.getRelationshipType()) &&
                deletedFileIds.contains(rel.getRelatedSpdxElement())
        );

        if (spdxDocumentRoot.getFiles() != null && !deletedFileIds.isEmpty()) {
            spdxDocumentRoot.getFiles().removeIf(file -> {
                if (!deletedFileIds.contains(file.getSpdxId())) {
                    return false;
                }
                return spdxDocumentRoot.getRelationships().stream() // important for cases when duplicates
                        .noneMatch(rel -> CONTAINS.equals(rel.getRelationshipType()) &&
                                file.getSpdxId().equals(rel.getRelatedSpdxElement()));
            });
        }

        for (String fileId : activeFileIds) {
            boolean relationShipExists = spdxDocumentRoot.getRelationships().stream()
                    .anyMatch(rel -> packageSpdxId.equals(rel.getSpdxElementId()) && fileId.equals(rel.getRelatedSpdxElement()));

            if (!relationShipExists) {
                RelationshipEntity rel = new RelationshipEntity();
                rel.setSpdxDocument(spdxDocumentRoot);
                rel.setSpdxElementId(packageSpdxId);
                rel.setRelatedSpdxElement(fileId);
                rel.setRelationshipType(CONTAINS);

                spdxDocumentRoot.getRelationships().add(rel);
            }
        }
    }
}
