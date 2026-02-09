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

import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FileFactory {
    private static final Logger log = LoggerFactory.getLogger(FileFactory.class);

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

        File file = new File();
        file.setProject(project);
        file.setFileName(fileName);

        file.setPhysicalPath(physicalPath);
        file.setProjectPath(projectPath);
        file.setArtifactPath(artifactPath);

        file.setIsDirectory(isDirectory);
        file.setParent(parentEntity);
        file.setInventoryItem(inventoryItem);

        return file;
    }

    public File createWithoutInventoryItem(Project project,
                                           String fileName,
                                           String physicalPath,
                                           String projectPath,
                                           String artifactPath,
                                           boolean isDirectory,
                                           File parentEntity) {
        return create(project, fileName, physicalPath, projectPath, artifactPath, isDirectory, parentEntity, null);
    }

    public File updateFileEntity(
            File file,
            Project project,
            String fileName,
            String physicalPath,
            String projectPath,
            boolean isDirectory,
            File parentEntity) {
        log.trace("Updating file entity of artifact {} with physical path {} and project path {}",
                file.getArtifactPath(), physicalPath, projectPath);
        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(physicalPath, "Physical path cannot be null");

        if (file.getFileName() == null || file.getFileName().isBlank())
            file.setFileName(fileName);

        file.setPhysicalPath(physicalPath);
        file.setProjectPath(projectPath);
        file.setIsDirectory(isDirectory);
        file.setParent(parentEntity);
        file.setReviewed(false);

        return file;
    }
}