/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileContentService {

    private static final Logger log = LogManager.getLogger(FileContentService.class);

    /**
     * Retrieves the content of a file.
     * If file path is not absolute, it is resolved relative to the root of the inventory item.
     *
     * @param codeLocation the object containing the file path and associated metadata
     * @param inventoryItem the inventory item of the code location
     */
    public FileResult getFileContentOfCodeLocation(CodeLocation codeLocation, InventoryItem inventoryItem) {
        String pathToView = codeLocation.getFilePath();
        log.debug("Attempting to view content for path to view: {}", pathToView);

        Path finalPath;

        try {
            Path relativeOrAbsolutePath = Paths.get(pathToView);

            if (relativeOrAbsolutePath.isAbsolute()) {
                finalPath = relativeOrAbsolutePath;
                log.debug("Path to view is absolute. Using as is: {}", finalPath);
            } else {
                log.debug("Path is relative. Looking for absolute path...");
                InventoryItem root = inventoryItem;
                while( root != null && root.getParent() != null){
                    root = root.getParent();
                }

                if (root == null || root.getBasePath() == null || root.getBasePath().isBlank()) {
                    log.error("No root base path found for relative path {}. Cannot view file.", pathToView);
                    return new FileResult.Failure("Cannot view file: The root project directory path is not set.");
                }

                Path rootBasePath = Paths.get(root.getBasePath());

                if (!rootBasePath.isAbsolute()) {
                    log.warn("Aborting: The root base path '{}' is not an absolute path.", rootBasePath);
                    return new FileResult.Failure("Configuration Error: Please ensure that the root base path is set " +
                            "correctly and that it is an absolute path.");
                }

                finalPath = rootBasePath.resolve(relativeOrAbsolutePath);
                log.debug("Resolved relative path '{}' against root '{}' to get final path '{}'", relativeOrAbsolutePath, rootBasePath, finalPath);
            }
            if (!Files.exists(finalPath) || !Files.isRegularFile(finalPath)) {
                log.warn("Aborting: File path '{}' does not exist or is not a regular file.", finalPath);
                return new FileResult.Failure("File Not Found: Please ensure that the file paths are correct and that" +
                        " the file exists locally");
            }

            log.info("Viewing file at path '{}'", finalPath);
            String content = Files.readString(finalPath);
            return new FileResult.Success(content, finalPath.toString());

        } catch (IOException e) {
            log.error("Error while viewing file at path '{}'", pathToView, e);
            return new FileResult.Failure("Error Reading File<br>Could not read the content of the file. Please check file permissions.");
        } catch (InvalidPathException e){
            log.error("Invalid path while viewing file at path '{}'", pathToView, e);
            return new FileResult.Failure("Invalid Path. The file path provided is not valid: pathToView");
        }
    }

    public FileResult getFileContent(String absolutePath) {
        try {
            Path path = Paths.get(absolutePath);

            String content = Files.readString(path);
            return new FileResult.Success(content, path.toString());

        } catch (IOException e) {
            log.error("Error reading file at path '{}'", absolutePath, e);
            return new FileResult.Failure("Error reading file. Please check file permissions: " + absolutePath);
        } catch (InvalidPathException e) {
            log.error("Invalid path '{}'", absolutePath, e);
            return new FileResult.Failure("Invalid file path: " + absolutePath);
        }
    }
}
