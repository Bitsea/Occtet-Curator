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

package eu.occtet.boc.cyclonedx.service;

import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class CleanUpService {

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private AppConfigurationRepository appConfigurationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(CleanUpService.class);

    /**
     * Cleans up the file tree associated with the given project
     * Executes fully before returning to ensure no race conditions with subsequent file creations.
     * @param project
     */
    public void cleanUpFileTree(Project project) {
        String globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                .map(AppConfiguration::getValue)
                .orElseThrow(() -> new RuntimeException("General Base Path not configured!"));
        String folderName = project.getProjectName() + "_" + project.getVersion();
        Path projectDir = Paths.get(globalBasePath).resolve(folderName);

        boolean hasFilesOnDisk = Files.exists(projectDir) && !isDirectoryEmpty(projectDir);
        boolean hasFilesInDb = fileRepository.existsByProject(project);

        if (!hasFilesOnDisk && !hasFilesInDb) {
            log.debug("Directory {} does not exist or is empty. Skipping filesystem cleanup.", projectDir);
        }log.info("Starting cleanup for project {} (Filesystem: {}, DB: {})",
                project.getProjectName(), hasFilesOnDisk, hasFilesInDb);

        // clean up data system
        if (hasFilesOnDisk) {
            log.debug("Deleting project directory from disk: {}", projectDir);
            deleteProjectDirectory(projectDir);
        } else {
            log.debug("Directory {} is empty or does not exist. Skipping disk cleanup.", projectDir);
        }

        // clean up DB
        if (hasFilesInDb) {
            log.debug("Deleting file records from database for project: {}", project.getProjectName());
            project.removeFiles();
            projectRepository.save(project);
            deleteFilesBatched(project);

            // cache must be cleared to avoid stale data issues in subsequent operations
            entityManager.flush(); // close write operations to the database
            entityManager.clear(); // persistence context cleared

            // invalidate l2 cache to ensure no stale data is served in future queries
            entityManager.getEntityManagerFactory().getCache().evictAll();
        }

        log.info("Finished cleanup for project: {}", project.getProjectName());
    }

    /**
     * Memory-safe directory deletion using FileVisitor (O(1) memory overhead).
     */
    private void deleteProjectDirectory(Path projectRoot) {
        if (projectRoot == null || !Files.exists(projectRoot)) {
            return;
        }

        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (NoSuchFileException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (NoSuchFileException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.warn("Could not access file during cleanup: {}, error: {}", file, exc != null ? exc.getMessage() : "unknown");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk/delete through the project directory: {} exception: {}", projectRoot, e.getMessage());
            throw new RuntimeException("Failed to delete project directory " + projectRoot, e);
        }
    }

    /**
     * Deletes all file-related records completely in batches.
     * NO @Transactional annotation here to allow committing each batch individually!
     */
    public void deleteFilesBatched(Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        Long projectId = project.getId();
        log.info("Starting fully batched deletion for project ID: {}", projectId);

        int batchSize = 10000; // for pure sql query this size is ideal
        int affectedCount;

        // delete copyright file links in batches
        log.debug("Deleting copyright file links in batches...");
        do {
            Integer count = transactionTemplate.execute(status ->
                    fileRepository.deleteCopyrightFileLinksBatchByProject(projectId, batchSize)
            );
            affectedCount = count != null ? count : 0;
            log.debug("Deleted {} copyright links in batch for project ID: {}", affectedCount, projectId);
        } while (affectedCount >= batchSize);

        // delete inventory item file links in batches
        log.debug("Deleting inventory item file links in batches...");
        do {
            Integer count = transactionTemplate.execute(status ->
                    fileRepository.deleteInventoryItemFileLinksBatchByProject(projectId, batchSize)
            );
            affectedCount = count != null ? count : 0;
            log.debug("Deleted {} inventory links in batch for project ID: {}", affectedCount, projectId);
        } while (affectedCount >= batchSize);

        // delete parent-child relationships in batches
        log.debug("Unlinking parent-child relationships in batches...");
        do {
            Integer count = transactionTemplate.execute(status ->
                    fileRepository.unlinkParentsBatchByProject(projectId, batchSize)
            );
            affectedCount = count != null ? count : 0;
            log.debug("Unlinked {} parent-child relations in batch for project ID: {}", affectedCount, projectId);
        } while (affectedCount >= batchSize);

        // delete file records in batches
        log.debug("Deleting file records in batches...");
        do {
            Integer count = transactionTemplate.execute(status ->
                    fileRepository.deleteBatchByProject(projectId, batchSize)
            );
            affectedCount = count != null ? count : 0;
            log.debug("Deleted {} file records in batch for project ID: {}", affectedCount, projectId);
        } while (affectedCount >= batchSize);

        log.info("Finished fully batched deletion for project ID: {}", projectId);
    }


    /**
     * Checks if a directory is empty in O(1) time without loading directory content into memory.
     */
    private boolean isDirectoryEmpty(Path path) {
        if (!Files.isDirectory(path)) {
            return true;
        }
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            log.warn("Failed to check if directory is empty: {}, assuming not empty to be safe.", path, e);
            return false;
        }
    }

}
