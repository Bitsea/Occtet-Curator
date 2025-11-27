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


    public void createEntitiesFromPath(Project project, Path rootPath) {
        try {
            log.debug("createEntitiesFromPath called with rootPath: {}", rootPath);
            // fileRepository.deleteByProject(project); determine whether if this is needed?
            log.debug("Starting the scan.");
            scanDir(project, rootPath.toFile(),null);
            log.debug("Scan of directory {} completed", rootPath);
            log.debug("Number of files created: {}", (int) fileRepository.countByProject(project));
        } catch (Exception e){
            log.error("Could not scan directory {} with error message: {}", rootPath, e.getMessage());
        }
    }

    // FIXME runs endlessly; check consumer it could be that the process method in downloadService is being called
    //  endlessly
    /**
     * This method recursively scans a directory and creates File entities for each file found.
     */
    private void scanDir(Project project, java.io.File directory, File parentEntity){
        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (Files.isSymbolicLink(file.toPath())) {
                log.debug("Skipping symbolic link: {}", file.getAbsolutePath());
                continue;
            }

            File fileEntity = fileFactory.create(
                    project, file.getName(), file.getAbsolutePath(),
                    getRelativePath(project, file), file.isDirectory(), parentEntity);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity);
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
