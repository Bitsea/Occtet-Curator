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
import java.nio.file.Paths;
import java.util.*;

/**
 * Service for handling operations related to File entities, such as scanning directories
 * and creating corresponding File entities within the context of a Project.
 */
@Service
public class FileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private FileFactory fileFactory;

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
    public void createEntitiesFromPath(Project project, Path rootPath, Boolean isMainPackage) {
        long start = System.currentTimeMillis();
        try {
            log.info("Starting scan for project {} in path {}", project.getId(), rootPath);

            Map<String, File> existingFilesMap = new HashMap<>();
            List<File> existingFiles = fileRepository.findAllByProject(project);

            for (File f : existingFiles) {
                existingFilesMap.put(f.getAbsolutePath(), f);
            }

            List<File> filesToSave = new ArrayList<>();
            java.io.File rootDirectory = rootPath.toFile();
            File rootEntity;

            File parentForRoot = getOrCreateDependencyFolder(project, isMainPackage, existingFilesMap, filesToSave);

            if (!existingFilesMap.containsKey(rootDirectory.getAbsolutePath())) {
                rootEntity = fileFactory.create(project, rootDirectory.getName(),
                        rootDirectory.getAbsolutePath(), getRelativePath(project, rootDirectory),
                        true, parentForRoot);

                filesToSave.add(rootEntity);
            } else {
                rootEntity = existingFilesMap.get(rootDirectory.getAbsolutePath());
            }

            scanDir(project, rootDirectory, rootEntity, filesToSave, existingFilesMap);

            if (!filesToSave.isEmpty()) {
                fileRepository.saveAll(filesToSave);
                fileRepository.flush();
            }

            log.info("Scan completed. Processed {} new files in {} ms",
                    filesToSave.size(), System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("Could not scan directory {}", rootPath, e);
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
                         List<File> filesToSave, Map<String, File> existingFilesMap) {

        java.io.File[] files = directory.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (Files.isSymbolicLink(file.toPath())
                    || file.getName().equals(".git")
                    || file.getName().equals(".idea")
                    || file.getName().equals("target")
                    //... add more exclusions if familiar with them
            )
                continue;

            if (existingFilesMap.containsKey(file.getAbsolutePath())) {
                continue;
            }

            File fileEntity = fileFactory.create(
                    project,
                    file.getName(),
                    file.getAbsolutePath(),
                    getRelativePath(project, file),
                    file.isDirectory(),
                    parentEntity
            );

            filesToSave.add(fileEntity);

            if (file.isDirectory()) {
                scanDir(project, file, fileEntity, filesToSave, existingFilesMap);
            }
        }
    }

    private String getRelativePath(Project project, java.io.File file){
        try {
            return FilenameUtils.separatorsToUnix(Path.of(project.getBasePath()).relativize(file.toPath()).toString());
        } catch (IllegalArgumentException e) {
            return file.getName();
        }
    }

    private File getOrCreateDependencyFolder(Project project, Boolean isMainPackage,
                                             Map<String, File> existingFilesMap, List<File> filesToSave) {
        if (Boolean.TRUE.equals(isMainPackage)) {
            return null;
        }
        String depFolderPath = Paths.get(project.getBasePath(), "dependencies").toString();
        if (existingFilesMap.containsKey(depFolderPath)) {
            return existingFilesMap.get(depFolderPath);
        }
        for (File f : filesToSave) {
            if (f.getAbsolutePath().equals(depFolderPath)) {
                return f;
            }
        }
        File depFolderEntity = fileFactory.create(project, "dependencies", depFolderPath,
                "dependencies", true, null);
        existingFilesMap.put(depFolderPath, depFolderEntity);
        filesToSave.add(depFolderEntity);
        log.debug("Created dependencies folder entity");
        return depFolderEntity;
    }
}
