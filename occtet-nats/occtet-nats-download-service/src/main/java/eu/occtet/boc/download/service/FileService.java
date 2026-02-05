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

import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.download.factory.FileFactory;
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

            List<File> oldExistingProjectFileCache = fileRepository.findAllByProject(project);
            log.debug("Loaded {} existing File entities for project {}", oldExistingProjectFileCache.size(), project.getId());


            List<File> batchBuffer = new ArrayList<>();
            int batchSize = 500;
            java.io.File rootDirectory = rootPath.toFile();

            Path projectRootPathObj = Paths.get(projectPath);
            Path projectParentAnchor = projectRootPathObj.getParent();
            if (projectParentAnchor == null) {
                projectParentAnchor = projectRootPathObj;
            }

            File parentForRoot = ensureParentHierarchy(project, rootDirectory.getParentFile(), oldExistingProjectFileCache,
                    batchBuffer, projectPath, inventoryItem);

            String calculatedArtifactPath = getRelativePath(rootPath, rootDirectory); // Relative to scan anchor
            String calculatedProjectPath = getRelativePath(projectParentAnchor, rootDirectory); // Relative to Project root

            String rootPhysicalPath = rootDirectory.getAbsolutePath();
            log.debug("physical path: {} creating root directory", rootDirectory.getAbsolutePath());
            File rootEntity = fileFactory.create(
                        project,
                        rootDirectory.getName(),
                        rootPhysicalPath,
                        calculatedProjectPath,
                        calculatedArtifactPath,
                        true,
                        parentForRoot,
                        inventoryItem
                );
            oldExistingProjectFileCache.add( rootEntity);


            addToBatch(rootEntity, batchBuffer, batchSize);
            scanDir(project, rootDirectory, rootEntity, batchBuffer, oldExistingProjectFileCache, batchSize, inventoryItem,
                    rootPath, projectParentAnchor);

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
                         List<File> batchBuffer, List<File> projectFileCache, int batchSize,
                         InventoryItem inventoryItem, Path relativeAnchor, Path projectRootAnchor) {
        log.debug("Calculated artifact path: {}, calculated project path: {} directory {}", relativeAnchor.getFileName(),
                projectRootAnchor.getFileName(), directory.getAbsoluteFile());

        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (shouldIgnore(file)) continue;

            String physicalPath = file.getAbsolutePath();

            String artifactPath = getRelativePath(relativeAnchor, file);
            String projectPath = getRelativePath(projectRootAnchor, file);
            log.debug("artifactPath {} and projectPath {}", artifactPath, projectPath);
            File existingFile = projectFileCache.stream()
                            .filter(f -> f.getArtifactPath().equals(artifactPath))
                            .findFirst()
                            .orElse(null);

            if (existingFile != null) {
                //for updating the data
                if(existingFile.getProjectPath()==null)
                    fileFactory.updateFileEntity(existingFile, project, physicalPath, projectPath,file.isDirectory(), parentEntity, inventoryItem);
                log.debug("File already exists in cache: {} with artefactPath {}", physicalPath, existingFile.getArtifactPath());
                addToBatch(existingFile, batchBuffer, batchSize);
                if (file.isDirectory()) {
                    scanDir(project, file, existingFile, batchBuffer, projectFileCache,
                            batchSize, inventoryItem, relativeAnchor, projectRootAnchor);
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
                    inventoryItem
            );

            projectFileCache.add(fileEntity);
            addToBatch(fileEntity, batchBuffer, batchSize);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, batchBuffer, projectFileCache,
                        batchSize, inventoryItem, relativeAnchor, projectRootAnchor);
            }
        }
    }

    private File ensureParentHierarchy(Project project, java.io.File startDirectory, List<File> oldFileCache,
                                       List<File> batchBuffer, String projectPath, InventoryItem inventoryItem) {
        if (startDirectory == null) return null;

        String currentPhysicalPath = startDirectory.getAbsolutePath();
        log.debug("Starting ensureParentHierarchy for path: {}", currentPhysicalPath);
        if (!currentPhysicalPath.startsWith(projectPath) || currentPhysicalPath.equals(projectPath)) {
            return null;
        }
        for(File f: oldFileCache) {
            if (f.getPhysicalPath()!= null && f.getPhysicalPath().equals(currentPhysicalPath)) {
                return f;
            }
        }

        File parentOfThis = ensureParentHierarchy(project, startDirectory.getParentFile(), oldFileCache,
                batchBuffer, projectPath, inventoryItem);

        Path projectRootPathObj = Paths.get(projectPath);
        Path projectParentAnchor = projectRootPathObj.getParent();
        if (projectParentAnchor == null) projectParentAnchor = projectRootPathObj;

        String relativeToProject = getRelativePath(projectParentAnchor, startDirectory);

        String relativeToRoot = getRelativePath(projectRootPathObj, startDirectory);
        log.debug("creating file with projectPath {} and artifactPath {}", relativeToProject, relativeToRoot);
        File newEntity = fileFactory.create(
                project,
                startDirectory.getName(),
                currentPhysicalPath,
                relativeToProject,
                relativeToRoot,
                true,
                parentOfThis,
                inventoryItem
        );

        oldFileCache.add( newEntity);
        addToBatch(newEntity, batchBuffer, 500);

        return newEntity;
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