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

import eu.occtet.boc.download.dao.FileRepository;
import eu.occtet.boc.download.factory.FileFactory;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.Project;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling operations related to File entities, such as scanning directories
 * and creating corresponding File entities within the context of a Project.
 */
@Service
public class FileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FileFactory fileFactory;
    @Autowired
    private FileRepository fileRepository;

    /**
     * Scans the specified path for files and directories within the context of a given project,
     * creates corresponding File entities, and saves them to the database. Any existing File
     * entities starting with the specified path are deleted prior to the scan to avoid duplicates.
     *
     * @param project the Project entity to which the files and directories belong
     * @param specificPathToScan the root path to scan for files and directories
     */
    @Transactional
    public void createEntitiesFromPath(Project project, Path specificPathToScan) {
        try {
            log.info("Starting scan for project {} in path {}", project.getId(), specificPathToScan);
            // just in case delete pre-existing files in this path to avoid duplicates
            fileRepository.deleteByProjectAndAbsolutePathStartingWith(project, specificPathToScan.toString());
            List<File> buffer = new ArrayList<>();
            scanDir(project, specificPathToScan.toFile(), null, buffer);
            if (!buffer.isEmpty()) {
                log.info("Saving {} files to database...", buffer.size());
                fileRepository.saveAll(buffer);
                log.info("Save complete.");
            }

        } catch (Exception e){
            log.error("Could not scan directory {}", specificPathToScan, e);
        }
    }

    /**
     * Recursively scans a given directory and populates the provided buffer with File entities
     * representing the files and subdirectories found. Skips symbolic links to avoid processing
     * invalid or redundant paths.
     *
     * @param project the Project entity associated with the directory being scanned
     */
    private void scanDir(Project project, java.io.File directory, File parentEntity, List<File> buffer) {
        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (Files.isSymbolicLink(file.toPath())) continue;

            File fileEntity = fileFactory.create(
                    project,
                    file.getName(),
                    file.getAbsolutePath(),
                    getRelativePath(project, file),
                    file.isDirectory(),
                    parentEntity
            );

            buffer.add(fileEntity);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, buffer);
            }
        }
    }

    private String getRelativePath(Project project, java.io.File file){
        Path basePath = Path.of(project.getBasePath());
        Path filePath = file.toPath();
        try {
            Path relativePath = basePath.relativize(filePath);
            return FilenameUtils.separatorsToUnix(relativePath.toString());
        } catch (IllegalArgumentException e) {
            return file.getName();
        }
    }
}
