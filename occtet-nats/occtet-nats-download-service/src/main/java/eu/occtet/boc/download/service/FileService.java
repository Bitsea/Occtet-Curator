/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.download.service;

import eu.occtet.boc.download.dao.CodeLocationRepository;
import eu.occtet.boc.download.dao.FileRepository;
import eu.occtet.boc.download.factory.FileFactory;
import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for scanning the file system and persisting the structure as {@link File} entities.
 * <p>
 * This service handles the mapping between physical files on disk and their database representation.
 * It employs a batch processing strategy to handle large directory trees efficiently without
 * exhausting memory.
 */
@Service
public class FileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private FileFactory fileFactory;
    @Autowired
    private CodeLocationRepository codeLocationRepository;

    @Value("${occtet.scanner.ignored-names:}")
    private List<String> ignoredNames = Collections.emptyList();

    /**
     * Scans a directory located at the given path and creates corresponding File entities.
     * <p>
     * This method is memory-optimized: it does not load the entire project file tree into memory.
     * Instead, it resolves the root anchor and then recursively persists children in batches.
     *
     * @param project       the Project entity to which the scanned files belong
     * @param inventoryItem the InventoryItem associated with this scan (if applicable)
     * @param rootPath      the physical path on disk to start scanning
     * @param isMainPackage flag indicating if this is the main package or a dependency
     */
    @Transactional
    public void createEntitiesFromPath(Project project, InventoryItem inventoryItem, Path rootPath, Boolean isMainPackage) {
        long start = System.currentTimeMillis();
        try {
            log.info("Starting scan for project {} in path {}", project.getId(), rootPath);

            // Prepare CodeLocations (Lookup Map)
            Map<String, CodeLocation> codeLocationMap = new HashMap<>();
            if (inventoryItem != null) {
                List<CodeLocation> cls = codeLocationRepository.findCodeLocationByInventoryItem(inventoryItem);
                for (CodeLocation cl : cls) {
                    codeLocationMap.put(cl.getFilePath(), cl);
                }
            }

            List<File> batchBuffer = new ArrayList<>();
            int batchSize = 500;
            java.io.File rootDirectory = rootPath.toFile();

            // Ensure the parent hierarchy exists (Anchor the scan to the project root)
            // We pass the batchBuffer so intermediate folders are created and saved if missing.
            File parentForRoot = ensureParentHierarchy(project, rootDirectory.getParentFile(), batchBuffer);

            // Handle the Root Entity of this scan
            String relativePath = getRelativePath(rootPath, rootDirectory);
            CodeLocation rootCodeLocation = determineCodeLocation(relativePath, codeLocationMap);

            File rootEntity = fileRepository.findByProjectAndAbsolutePath(project, rootDirectory.getAbsolutePath());

            if (rootEntity == null) {
                rootEntity = fileFactory.create(
                        project,
                        rootDirectory.getName(),
                        rootDirectory.getAbsolutePath(),
                        relativePath,
                        true,
                        parentForRoot,
                        rootCodeLocation != null ? inventoryItem : null,
                        rootCodeLocation
                );
                addToBatch(rootEntity, batchBuffer, batchSize);
            } else {
                // Update existing linkage if necessary
                updateFileLinkage(rootEntity, rootCodeLocation, batchBuffer, batchSize);
            }

            // Recursive Scan starting from the root
            scanDir(project, rootDirectory, rootEntity, batchBuffer, batchSize, inventoryItem, rootPath, codeLocationMap);

            // Final Flush
            if (!batchBuffer.isEmpty()) {
                fileRepository.saveAll(batchBuffer);
                fileRepository.flush();
            }

            log.info("Scan completed. Processed files in {} ms", System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("Could not scan directory {}", rootPath, e);
            throw new RuntimeException("Scan failed", e);
        }
    }

    /**
     * Recursively scans a directory and adds found files to the persistence batch.
     */
    private void scanDir(Project project, java.io.File directory, File parentEntity,
                         List<File> batchBuffer, int batchSize,
                         InventoryItem inventoryItem, Path relativeAnchor, Map<String, CodeLocation> codeLocationMap) {

        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (shouldIgnore(file)) continue;

            String absPath = file.getAbsolutePath();

            File existingFile = fileRepository.findByProjectAndAbsolutePath(project, absPath);

            String calculatedRelativePath = getRelativePath(relativeAnchor, file);
            CodeLocation codeLocation = determineCodeLocation(calculatedRelativePath, codeLocationMap);

            if (existingFile != null) {
                updateFileLinkage(existingFile, codeLocation, batchBuffer, batchSize);
                if (file.isDirectory()) {
                    scanDir(project, file, existingFile, batchBuffer, batchSize, inventoryItem, relativeAnchor, codeLocationMap);
                }
                continue;
            }

            File fileEntity = fileFactory.create(
                    project,
                    file.getName(),
                    absPath,
                    calculatedRelativePath,
                    file.isDirectory(),
                    parentEntity,
                    codeLocation != null ? inventoryItem : null,
                    codeLocation
            );

            addToBatch(fileEntity, batchBuffer, batchSize);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, batchBuffer, batchSize, inventoryItem, relativeAnchor, codeLocationMap);
            }
        }
    }

    /**
     * Ensures that the folder structure leading up to the scan root exists in the database.
     * <p>
     * This climbs up the directory tree until it finds a folder that is already persisted
     * (or hits the project base path) and creates the missing links.
     */
    private File ensureParentHierarchy(Project project, java.io.File startDirectory, List<File> batchBuffer) {
        if (startDirectory == null) return null;

        String projectBasePath = project.getBasePath();
        String currentPath = startDirectory.getAbsolutePath();

        // Stop if we went above the project base path
        if (!currentPath.startsWith(projectBasePath) || currentPath.equals(projectBasePath)) {
            return null;
        }

        // Check if this specific parent already exists
        File existing = fileRepository.findByProjectAndAbsolutePath(project, currentPath);
        if (existing != null) {
            return existing;
        }

        // Recursively ensure the parent of this directory exists
        File parentOfThis = ensureParentHierarchy(project, startDirectory.getParentFile(), batchBuffer);

        // Create the missing directory entity
        File newEntity = fileFactory.create(
                project,
                startDirectory.getName(),
                currentPath,
                getRelativePath(Paths.get(projectBasePath), startDirectory),
                true,
                parentOfThis,
                null,
                null
        );

        addToBatch(newEntity, batchBuffer, 500);

        return newEntity;
    }

    private CodeLocation determineCodeLocation(String currentPath, Map<String, CodeLocation> map) {
        if (map.isEmpty()) return null;

        if (map.containsKey(currentPath)) {
            return map.get(currentPath);
        }

        // Climb up relative path to find nearest CodeLocation match
        Path pathObj = Paths.get(currentPath);
        Path parent = pathObj.getParent();

        while (parent != null) {
            String parentStr = FilenameUtils.separatorsToUnix(parent.toString());
            if (map.containsKey(parentStr)) {
                return map.get(parentStr);
            }
            parent = parent.getParent();
        }

        return null;
    }

    private void updateFileLinkage(File fileEntity, CodeLocation newCodeLocation, List<File> batch, int batchSize) {
        if (newCodeLocation == null) return;

        CodeLocation current = fileEntity.getCodeLocation();
        if (current != null && current.getId().equals(newCodeLocation.getId())) {
            return;
        }

        fileEntity.setCodeLocation(newCodeLocation);
        addToBatch(fileEntity, batch, batchSize);
    }

    private void addToBatch(File entity, List<File> batch, int batchSize) {
        batch.add(entity);

        if (batch.size() >= batchSize) {
            fileRepository.saveAll(batch);
            fileRepository.flush();
            batch.clear();
        }
    }

    private boolean shouldIgnore(java.io.File file) {
        return Files.isSymbolicLink(file.toPath()) || ignoredNames.contains(file.getName());
    }

    private String getRelativePath(Path anchor, java.io.File file) {
        try {
            return FilenameUtils.separatorsToUnix(anchor.relativize(file.toPath()).toString());
        } catch (IllegalArgumentException e) {
            return file.getName();
        }
    }
}