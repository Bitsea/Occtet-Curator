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
 * Service responsible for managing file system scans and synchronizing the directory structure
 * with the database {@link File} entities.
 * <p>
 * This service is critical for mapping the physical downloaded source code to the logical
 * data model, ensuring that every file and folder is represented as an entity and linked
 * to the correct {@link InventoryItem} where applicable.
 * </p>
 *
 * <p>
 * <b>Workflow Steps:</b>
 * <ol>
 * <li><b>Initialization & Caching:</b> To ensure high performance, the service pre-loads:
 * <ul>
 * <li>All existing {@link File} entities for the project into a memory map (Path &rarr; Entity) to avoid N+1
 * database
 * queries.</li>
 * <li>All relevant {@link CodeLocation} entities into a map to enable fast O(1) lookups for file ownership.</li>
 * </ul>
 * </li>
 * <li><b>Root Processing:</b>
 * <ul>
 * <li>Identifies the root directory of the scan.</li>
 * <li>Ensures the full parent hierarchy (e.g., <code>dependencies/lib-name/version/</code>) exists in the database before processing children.</li>
 * </ul>
 * </li>
 * <li><b>Recursive Scan:</b> Traverses the file system directory tree recursively.</li>
 * <li><b>Entity Resolution (Create vs. Update):</b> For each file found:
 * <ul>
 * <li>Checks the memory cache to see if the entity already exists.</li>
 * <li><b>If New:</b> Creates a new {@link File} entity and adds it to the batch buffer.</li>
 * <li><b>If Existing:</b> Updates the entity, specifically checking if the ownership (CodeLocation) needs to be updated (e.g., claiming a file for a specific InventoryItem).</li>
 * </ul>
 * </li>
 * <li><b>Ownership Linking:</b> Determines the correct {@link CodeLocation} for each file by matching the file path against the pre-loaded CodeLocation map (supporting partial/folder matching).</li>
 * <li><b>Batch Persistence:</b> flushes changes to the database in batches (e.g., every 500 items) to optimize transaction performance and reduce memory usage.</li>
 * </ol>
 * </p>
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

    @Value("${occtet.scanner.ignored-names}")
    private List<String> ignoredNames;

    /**
     * Scans a directory located at the given path and creates corresponding File entities
     * within the provided project. All discovered files and directories are stored as entities.
     * <p>
     * The method ensures that symbolic links are skipped during the scan to prevent processing
     * invalid or redundant paths. It also persists the created entities using the associated repository.
     *
     * @param project the Project entity to which the scanned files and directories belong
     * @param rootPath the root path of the directory to be scanned and processed
     */
    @Transactional
    public void createEntitiesFromPath(Project project, InventoryItem inventoryItem, Path rootPath, Boolean isMainPackage) {
        long start = System.currentTimeMillis();
        try {
            log.info("Starting scan for project {} in path {}", project.getId(), rootPath);

            Map<String, File> projectFileCache = fileRepository.findAllByProject(project).stream()
                    .collect(Collectors.toMap(File::getAbsolutePath, Function.identity(), (a, b) -> a));

            Map<String, CodeLocation> codeLocationMap = new HashMap<>();
            if (inventoryItem != null) {
                List<CodeLocation> cls = codeLocationRepository.findCodeLocationsByInventoryItem(inventoryItem);
                for (CodeLocation cl : cls) {
                    codeLocationMap.put(cl.getFilePath(), cl);
                }
            }

            List<File> batchBuffer = new ArrayList<>();
            int batchSize = 500;
            java.io.File rootDirectory = rootPath.toFile();

            // Ensure parent hierarchy exists (using Cache)
            File parentForRoot = ensureParentHierarchy(project, rootDirectory.getParentFile(), projectFileCache, batchBuffer);

            File rootEntity;
            String relativePath = getRelativePath(rootPath, rootDirectory);
            CodeLocation rootCodeLocation = determineCodeLocation(relativePath, codeLocationMap);

            if (!projectFileCache.containsKey(rootDirectory.getAbsolutePath())) {
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
                projectFileCache.put(rootEntity.getAbsolutePath(), rootEntity);
            } else {
                rootEntity = projectFileCache.get(rootDirectory.getAbsolutePath());
                updateFileLinkage(rootEntity, rootCodeLocation, batchBuffer, batchSize);
            }

            // Recursive Scan
            scanDir(project, rootDirectory, rootEntity, batchBuffer, projectFileCache, batchSize, inventoryItem, rootPath, codeLocationMap);

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
     * Recursively scans a given directory and populates the provided buffer with File entities
     * representing the files and subdirectories found. Skips symbolic links to avoid processing
     * invalid or redundant paths.
     *
     * @param project the Project entity associated with the directory being scanned
     */
    private void scanDir(Project project, java.io.File directory, File parentEntity,
                         List<File> batchBuffer, Map<String, File> projectFileCache, int batchSize,
                         InventoryItem inventoryItem, Path relativeAnchor, Map<String, CodeLocation> codeLocationMap) {
        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (shouldIgnore(file)) continue;

            String absPath = file.getAbsolutePath();
            String calculatedRelativePath = getRelativePath(relativeAnchor, file);
            CodeLocation codeLocation = determineCodeLocation(calculatedRelativePath, codeLocationMap);

            if (projectFileCache.containsKey(absPath)) {
                File existingFile = projectFileCache.get(absPath);
                updateFileLinkage(existingFile, codeLocation, batchBuffer, batchSize);

                if (file.isDirectory()) {
                    scanDir(project, file, existingFile, batchBuffer, projectFileCache, batchSize, inventoryItem, relativeAnchor, codeLocationMap);
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
            projectFileCache.put(absPath, fileEntity);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, batchBuffer, projectFileCache, batchSize, inventoryItem, relativeAnchor, codeLocationMap);
            }
        }
    }

    private File ensureParentHierarchy(Project project, java.io.File startDirectory, Map<String, File> projectFileCache, List<File> batchBuffer) {
        if (startDirectory == null) return null;

        String projectBasePath = project.getBasePath();
        if (!startDirectory.getAbsolutePath().startsWith(projectBasePath) || startDirectory.getAbsolutePath().equals(projectBasePath)) {
            return null;
        }

        File parentEntity = ensureParentHierarchy(project, startDirectory.getParentFile(), projectFileCache, batchBuffer);
        String currentPath = startDirectory.getAbsolutePath();

        if (projectFileCache.containsKey(currentPath)) {
            return projectFileCache.get(currentPath);
        }

        File newEntity = fileFactory.create(
                project,
                startDirectory.getName(),
                currentPath,
                getRelativePath(Paths.get(projectBasePath), startDirectory),
                true,
                parentEntity,
                null,
                null
        );

        addToBatch(newEntity, batchBuffer, 500);
        projectFileCache.put(currentPath, newEntity);
        return newEntity;
    }

    /**
     * Determines the correct CodeLocation.<p>
     * Checks for exact match first. If not found, climbs up the directory tree
     * to find a matching folder CodeLocation (Partial Match).
     */
    private CodeLocation determineCodeLocation(String currentPath, Map<String, CodeLocation> map) {
        if (map.isEmpty()) return null;

        if (map.containsKey(currentPath)) {
            return map.get(currentPath);
        }

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

    /**
     * Updates the file's CodeLocation if it differs from the calculated one. <p>
     * In most cases, there shouldn't be an old one set instead it should be null.
     */
    private void updateFileLinkage(File fileEntity, CodeLocation newCodeLocation, List<File> batch, int batchSize) {
        if (newCodeLocation == null) return;

        CodeLocation current = fileEntity.getCodeLocation();
        if (current != null && current.getId().equals(newCodeLocation.getId())) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Updating linkage for file {}: Old CL={} -> New CL={}",
                    fileEntity.getFileName(),
                    (current != null ? current.getId() : "null"),
                    newCodeLocation.getId());
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
        if (Files.isSymbolicLink(file.toPath())) {
            return true;
        }
        if (ignoredNames.contains(file.getName())) {
            return true;
        }
        return false;
    }

    private String getRelativePath(Path anchor, java.io.File file) {
        try {
            return FilenameUtils.separatorsToUnix(anchor.relativize(file.toPath()).toString());
        } catch (IllegalArgumentException e) {
            return file.getName();
        }
    }

    private File getOrCreateDependencyFolder(Project project, Boolean isMainPackage,
                                             Set<String> existingPaths, List<File> batchBuffer) {
        if (Boolean.TRUE.equals(isMainPackage)) return null;

        String depFolderPath = Paths.get(project.getBasePath(), "dependencies").toString();

        for (File f : batchBuffer) {
            if (f.getAbsolutePath().equals(depFolderPath)) return f;
        }
        if (existingPaths.contains(depFolderPath)) {
            return fileRepository.findByProjectAndAbsolutePath(project, depFolderPath);
        }

        File depFolder = fileFactory.create(project, "dependencies", depFolderPath,
                "dependencies", true, null, null, null);
        addToBatch(depFolder, batchBuffer, 500);
        existingPaths.add(depFolderPath);

        return depFolder;
    }
}
