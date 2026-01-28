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

import eu.occtet.boc.dao.CodeLocationRepository;
import eu.occtet.boc.dao.FileRepository;
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
     * <b>Performance Note:</b> This method pre-loads all existing files for the project into a Map.
     * This reduces the time complexity of checking for existing files from O(N*Query) to O(1).
     *
     * @param project       the Project entity
     * @param inventoryItem the associated InventoryItem
     * @param rootPath      the physical path on disk
     * @param projectPath   the location of the downloaded project
     */
    @Transactional
    public void createEntitiesFromPath(Project project, InventoryItem inventoryItem, Path rootPath,
                                       String projectPath) {
        long start = System.currentTimeMillis();
        try {
            log.info("Starting scan for project {} in path {}", project.getId(), rootPath);

            Map<String, File> projectFileCache = fileRepository.findAllByProject(project).stream()
                    .collect(Collectors.toMap(File::getPhysicalPath, Function.identity(), (a, b) -> a));

            Map<String, CodeLocation> codeLocationMap = new HashMap<>();
            if (inventoryItem != null) {
                List<CodeLocation> cls = codeLocationRepository.findByInventoryItem(inventoryItem);
                for (CodeLocation cl : cls) {
                    codeLocationMap.put(cl.getFilePath(), cl);
                }
            }

            List<File> batchBuffer = new ArrayList<>();
            int batchSize = 500;
            java.io.File rootDirectory = rootPath.toFile();

            Path projectRootPathObj = Paths.get(projectPath);
            Path projectParentAnchor = projectRootPathObj.getParent();
            if (projectParentAnchor == null) {
                projectParentAnchor = projectRootPathObj;
            }

            File parentForRoot = ensureParentHierarchy(project, rootDirectory.getParentFile(), projectFileCache,
                    batchBuffer, projectPath);

            String calculatedArtifactPath = getRelativePath(rootPath, rootDirectory); // Relative to scan anchor
            String calculatedProjectPath = getRelativePath(projectParentAnchor, rootDirectory); // Relative to Project root

            CodeLocation rootCodeLocation = determineCodeLocation(calculatedArtifactPath, codeLocationMap);
            String rootPhysicalPath = rootDirectory.getAbsolutePath();

            File rootEntity = projectFileCache.get(rootPhysicalPath);

            if (rootEntity == null) {
                rootEntity = fileFactory.create(
                        project,
                        rootDirectory.getName(),
                        rootPhysicalPath,
                        calculatedProjectPath,
                        calculatedArtifactPath,
                        true,
                        parentForRoot,
                        rootCodeLocation != null ? inventoryItem : null,
                        rootCodeLocation
                );
                projectFileCache.put(rootPhysicalPath, rootEntity);
                addToBatch(rootEntity, batchBuffer, batchSize);
            } else {
                updateFileLinkage(rootEntity, rootCodeLocation, batchBuffer, batchSize);
            }

            scanDir(project, rootDirectory, rootEntity, batchBuffer, projectFileCache, batchSize, inventoryItem,
                    rootPath, projectParentAnchor, codeLocationMap);

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

    private void scanDir(Project project, java.io.File directory, File parentEntity,
                         List<File> batchBuffer, Map<String, File> projectFileCache, int batchSize,
                         InventoryItem inventoryItem, Path relativeAnchor, Path projectRootAnchor,
                         Map<String, CodeLocation> codeLocationMap) {
        log.debug("Scanning directory {}", directory.getAbsolutePath());
        log.debug("Calculated artifact path: {}, calculated project path: {}", relativeAnchor.getFileName(),
                projectRootAnchor.getFileName());

        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (shouldIgnore(file)) continue;

            String physicalPath = file.getAbsolutePath();

            String artifactPath = getRelativePath(relativeAnchor, file);
            String projectPath = getRelativePath(projectRootAnchor, file);

            CodeLocation codeLocation = determineCodeLocation(artifactPath, codeLocationMap);

            File existingFile = projectFileCache.get(physicalPath);

            if (existingFile != null) {
                updateFileLinkage(existingFile, codeLocation, batchBuffer, batchSize);
                if (file.isDirectory()) {
                    scanDir(project, file, existingFile, batchBuffer, projectFileCache, batchSize, inventoryItem, relativeAnchor, projectRootAnchor, codeLocationMap);
                }
                continue;
            }

            File fileEntity = fileFactory.create(
                    project,
                    file.getName(),
                    physicalPath,
                    projectPath,
                    artifactPath,
                    file.isDirectory(),
                    parentEntity,
                    codeLocation != null ? inventoryItem : null,
                    codeLocation
            );

            projectFileCache.put(physicalPath, fileEntity);
            addToBatch(fileEntity, batchBuffer, batchSize);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, batchBuffer, projectFileCache, batchSize, inventoryItem, relativeAnchor, projectRootAnchor, codeLocationMap);
            }
        }
    }

    private File ensureParentHierarchy(Project project, java.io.File startDirectory, Map<String, File> projectFileCache,
                                       List<File> batchBuffer, String projectPath) {
        if (startDirectory == null) return null;

        String currentPhysicalPath = startDirectory.getAbsolutePath();

        if (!currentPhysicalPath.startsWith(projectPath) || currentPhysicalPath.equals(projectPath)) {
            return null;
        }

        if (projectFileCache.containsKey(currentPhysicalPath)) {
            return projectFileCache.get(currentPhysicalPath);
        }

        File parentOfThis = ensureParentHierarchy(project, startDirectory.getParentFile(), projectFileCache,
                batchBuffer, projectPath);

        Path projectRootPathObj = Paths.get(projectPath);
        Path projectParentAnchor = projectRootPathObj.getParent();
        if (projectParentAnchor == null) projectParentAnchor = projectRootPathObj;

        String relativeToProject = getRelativePath(projectParentAnchor, startDirectory);

        String relativeToRoot = getRelativePath(projectRootPathObj, startDirectory);

        File newEntity = fileFactory.create(
                project,
                startDirectory.getName(),
                currentPhysicalPath,
                relativeToProject,
                relativeToRoot,
                true,
                parentOfThis,
                null,
                null
        );

        projectFileCache.put(currentPhysicalPath, newEntity);
        addToBatch(newEntity, batchBuffer, 500);

        return newEntity;
    }

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