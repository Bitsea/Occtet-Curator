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

import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
public class FileFactory {

    public File create(Project project,
                       String fileName,
                       String absolutePath,
                       String relativePath,
                       boolean isDirectory,
                       File parentEntity,
                       InventoryItem inventoryItem,
                       CodeLocation codeLocation) {

        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(absolutePath, "Absolute path cannot be null");

        File file = new File();
        file.setProject(project);
        file.setFileName(fileName);
        file.setAbsolutePath(absolutePath);
        file.setRelativePath(relativePath);
        file.setIsDirectory(isDirectory);
        file.setParent(parentEntity);
        file.setInventoryItem(inventoryItem);
        file.setCodeLocation(codeLocation);

        file.setReviewed(false);

        return file;
    }

    public File create(Project project, String fileName, String absolutePath, String relativePath, boolean isDirectory) {
        return create(project, fileName, absolutePath, relativePath, isDirectory, null, null, null);
    }
}
