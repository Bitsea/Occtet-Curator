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

package eu.occtet.boc.spdx.service;

import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.File;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CleanUpService {

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private AppConfigurationRepository appConfigurationRepository;
    @Autowired
    private ProjectRepository projectRepository;

    private static final String SAFE_FILENAME_REGEX = "[^a-zA-Z0-9.\\-_]";

    private static final Logger log = LoggerFactory.getLogger(CleanUpService.class);


    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Cleans up the file tree associated with the given project synchronously.
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
        }
        log.info("Starting cleanup for project {} (Filesystem: {}, DB: {})",
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
     * Deletes file entities in chunks to prevent transaction timeouts and memory overflow.
     */
    @Transactional
    public void deleteFilesBatched(Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        Long projectId = project.getId();

        // 1. Delete associated link records from join tables first to avoid FK constraint violations
        fileRepository.deleteCopyrightFileLinksByProject(projectId);
        fileRepository.deleteInventoryItemFileLinksByProject(projectId);

        // 2. Unlink parent-child relationships within the file table
        fileRepository.unlinkParentsByProject(projectId);

        // 3. Delete file rows in batches to prevent locking tables for too long and transaction timeouts
        int batchSize = 5000;
        int deletedCount;

        do {
            deletedCount = fileRepository.deleteBatchByProject(projectId, batchSize);
        } while (deletedCount >= batchSize);
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
