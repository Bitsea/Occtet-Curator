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

package eu.occtet.boc.download.factory;

import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.download.service.DownloadService;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FileFactory {
    private static final Logger log = LoggerFactory.getLogger(FileFactory.class);

    @Autowired
    private FileRepository fileRepository;


    public File create(Project project,
                       String fileName,
                       String physicalPath,
                       String projectPath,
                       String artifactPath,
                       boolean isDirectory,
                       File parentEntity,
                       InventoryItem inventoryItem) {

        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(physicalPath, "Physical path cannot be null");

        File file = fileRepository.findByArtifactPathAndFileName(artifactPath, fileName);
        if(file== null){
            log.debug("File {} not found in repository, creating new one.", fileName);
            file = new File();
            file.setFileName(fileName);
            file.setArtifactPath(artifactPath);
            file.setProject(project);
        }

        file.setPhysicalPath(physicalPath);
        file.setProjectPath(projectPath);

        file.setIsDirectory(isDirectory);
        file.setParent(parentEntity);
        file.setInventoryItem(inventoryItem);

        file.setReviewed(false);

        return file;
    }


    public File updateFileEntity(File file, Project project,
        String physicalPath,
        String projectPath,
        boolean isDirectory,
        File parentEntity,
        InventoryItem inventoryItem) {

            Objects.requireNonNull(project, "Project cannot be null");
            Objects.requireNonNull(physicalPath, "Physical path cannot be null");
            log.debug("updating file with filename {} and inventoryItem {}", file.getFileName(), inventoryItem.getInventoryName());

            file.setPhysicalPath(physicalPath);
            file.setProjectPath(projectPath);

            file.setIsDirectory(isDirectory);
            file.setParent(parentEntity);
            file.setInventoryItem(inventoryItem);

            file.setReviewed(false);

            return file;

    }
}