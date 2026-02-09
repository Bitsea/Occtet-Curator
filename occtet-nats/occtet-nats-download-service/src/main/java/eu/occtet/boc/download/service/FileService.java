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
    private final int BATCHSIZE = 500;

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

            List<File> existingFiles = fileRepository.findAllByProject(project);
            log.debug("Loaded {} existing File entities for project {}", existingFiles.size(), project.getId());

            Map<String, File> filesCreatedInSpdxService = new HashMap<>(); // uses artifact path as key
            Map<String, File> filesCreatedOrUpdatedInFileService = new HashMap<>(); // uses physical path as key

            for (File file : existingFiles){
                if (file.getArtifactPath() != null && file.getPhysicalPath() == null)
                    filesCreatedInSpdxService.put(file.getArtifactPath(), file);
                else if (file.getPhysicalPath() != null)
                    filesCreatedOrUpdatedInFileService.put(file.getPhysicalPath(), file);
            }

            List<File> batchBuffer = new ArrayList<>();

            Path projectRootPathObj = Paths.get(projectPath);
            Path projectParentAnchor = projectRootPathObj.getParent();
            if (projectParentAnchor == null) {
                projectParentAnchor = projectRootPathObj;
            }

            java.io.File rootDirectory = rootPath.toFile();

            File parentForRoot = ensureParentHierarchy(
                    project,
                    rootDirectory.getParentFile(),
                    filesCreatedOrUpdatedInFileService,
                    batchBuffer,
                    projectPath,
                    inventoryItem
            );

            String calculatedArtifactPath = getRelativePath(rootPath, rootDirectory); // Relative to scan anchor
            String calculatedProjectPath = getRelativePath(projectParentAnchor, rootDirectory); // Relative to Project root
            String rootPhysicalPath = rootDirectory.getAbsolutePath();

            File rootEntity = filesCreatedOrUpdatedInFileService.get(rootPhysicalPath);
            if (rootEntity == null) {
                log.debug("physical path: {} creating root directory", rootDirectory.getAbsolutePath());
                rootEntity = fileFactory.create(
                        project,
                        rootDirectory.getName(),
                        rootPhysicalPath,
                        calculatedProjectPath,
                        calculatedArtifactPath,
                        true,
                        parentForRoot,
                        inventoryItem
                );
                filesCreatedOrUpdatedInFileService.put(rootPhysicalPath, rootEntity);
                addToBatch(rootEntity, batchBuffer, BATCHSIZE);
            }

            scanDir(
                    project,
                    rootDirectory,
                    rootEntity,
                    batchBuffer,
                    filesCreatedOrUpdatedInFileService,
                    filesCreatedInSpdxService,
                    BATCHSIZE,
                    inventoryItem,
                    rootPath,
                    projectParentAnchor
            );

            if (!batchBuffer.isEmpty()) {
                fileRepository.saveAll(batchBuffer);
                fileRepository.flush();
                log.debug("Saved {} File entities in batch", batchBuffer.size());
            }

            log.info("Scan completed. Processed files in {} ms", System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("Could not scan directory {}", rootPath, e);
            throw new RuntimeException("Scan failed", e);
        }
    }

    /**
     * Recursively scans a directory and creates/updates File entities.
     *
     * @param project the Project entity
     * @param directory the directory to scan
     * @param parentEntity the parent File entity
     * @param batchBuffer the batch buffer for saving entities
     * @param filesInFileService files that already have physicalPath (created/updated in FileService)
     * @param filesInSpdxService files that only have artifactPath (pre-created by SPDX service)
     * @param batchSize the batch size for flushing
     * @param inventoryItem the associated InventoryItem
     * @param relativeAnchor anchor for calculating artifactPath
     * @param projectRootAnchor anchor for calculating projectPath
     */
    private void scanDir(Project project,
                         java.io.File directory,
                         File parentEntity,
                         List<File> batchBuffer,
                         Map<String, File> filesInFileService,
                         Map<String, File> filesInSpdxService,
                         int batchSize,
                         InventoryItem inventoryItem,
                         Path relativeAnchor,
                         Path projectRootAnchor) {

        java.io.File[] files = directory.listFiles();
        if (files == null) {
            log.debug("No files found in directory {}", directory.getAbsolutePath());
            return;
        }

        for (java.io.File file : files) {
            if (shouldIgnore(file)) {
                log.trace("Ignoring file {}", file.getAbsolutePath());
                continue;
            }

            String physicalPath = file.getAbsolutePath();
            String artifactPath = getRelativePath(relativeAnchor, file);
            String projectPath = getRelativePath(projectRootAnchor, file);

            File fileEntity = filesInFileService.get(physicalPath);

            // Scenario 1
            if (fileEntity != null) {
                log.trace("Reusing existing file entity: {}", physicalPath);
                if (file.isDirectory()){
                    scanDir(project, file, fileEntity, batchBuffer, filesInFileService, filesInSpdxService,
                            batchSize, inventoryItem, relativeAnchor, projectRootAnchor);
                }
                continue;
            }

            // Scenario 2
            fileEntity = filesInSpdxService.get(artifactPath);
            if (fileEntity != null) {
                log.trace("Found pre-created SPDX entity for artifact path: {} - updating with physical path: {}", artifactPath, physicalPath);
                fileEntity = fileFactory.updateFileEntity(
                        fileEntity,
                        project,
                        file.getName(),
                        physicalPath,
                        projectPath,
                        file.isDirectory(),
                        parentEntity);

                filesInSpdxService.remove(artifactPath);
                filesInFileService.put(physicalPath, fileEntity);

                addToBatch(fileEntity, batchBuffer, batchSize);

                if (file.isDirectory()){
                    scanDir(project, file, fileEntity, batchBuffer, filesInFileService, filesInSpdxService,
                            batchSize, inventoryItem, relativeAnchor, projectRootAnchor);
                }
                continue;
            }

            // Scenario 3
            log.debug("Creating new File entity: {}", physicalPath);
            fileEntity = fileFactory.createWithoutInventoryItem(
                    project,
                    file.getName(),
                    physicalPath,
                    projectPath,
                    artifactPath,
                    file.isDirectory(),
                    parentEntity
            );
            filesInFileService.put(physicalPath, fileEntity);
            addToBatch(fileEntity, batchBuffer, batchSize);
            if (file.isDirectory()){
                scanDir(project, file, fileEntity, batchBuffer, filesInFileService, filesInSpdxService,
                        batchSize, inventoryItem, relativeAnchor, projectRootAnchor);
            }
        }
    }


    /**
     * Ensures that all parent directories in the hierarchy exist as File entities.
     * Works recursively from the given directory up to the project root.
     */
    private File ensureParentHierarchy(Project project,
                                       java.io.File directory,
                                       Map<String, File> filesInFileService,
                                       List<File> batchBuffer,
                                       String projectPath,
                                       InventoryItem inventoryItem) {
        if (directory == null) {
            return null;
        }
        String currentPhysicalPath = directory.getAbsolutePath();
        log.trace("Ensuring parent hierarchy for path: {}", currentPhysicalPath);

        if (!currentPhysicalPath.startsWith(projectPath)) {
            log.trace("Path outside project scope, stopping");
            return null;
        }
        if (currentPhysicalPath.equals(projectPath)) {
            log.trace("Reached project root, stopping");
            return null;
        }

        File existingFile = filesInFileService.get(currentPhysicalPath);
        if (existingFile != null) {
            log.trace("Parent directory already exists: {}", currentPhysicalPath);
            return existingFile;
        }

        Path projectRootPathObj = Paths.get(projectPath);
        Path projectParentAnchor = projectRootPathObj.getParent();
        if (projectParentAnchor == null) projectParentAnchor = projectRootPathObj;

        String artifactPath = getRelativePath(projectRootPathObj, directory);
        String relativeProjectPath = getRelativePath(projectParentAnchor, directory);

        File parentFile = ensureParentHierarchy(
                project,
                directory.getParentFile(),
                filesInFileService,
                batchBuffer,
                projectPath,
                inventoryItem
        );

        log.debug("Creating parent directory entity: {} (artifactPath: {})",
                directory.getName(), artifactPath);
        File newEntity = fileFactory.create(
                project,
                directory.getName(),
                currentPhysicalPath,
                relativeProjectPath,
                artifactPath,
                true,
                parentFile,
                inventoryItem
        );

        filesInFileService.put(currentPhysicalPath, newEntity);
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