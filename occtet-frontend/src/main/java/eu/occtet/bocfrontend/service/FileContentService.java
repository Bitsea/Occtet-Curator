/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service class for managing file contents and resolving file paths.
 * This service provides methods to retrieve the content of files and
 * to locate files either locally or through a database lookup.
 */
@Service
public class FileContentService {

    private static final Logger log = LogManager.getLogger(FileContentService.class);

    @Autowired
    private FileRepository fileRepository;

    /**
     * Attempts to retrieve the content of a file based on the given {@link CodeLocation} and {@link InventoryItem}.
     * The method first checks if the resolved file path exists locally. If not, it attempts to resolve the file through
     * a database lookup using the associated inventory item.
     *
     * @param codeLocation the code location entity containing file path and other metadata
     * @param inventoryItem the inventory item associated with the project for database lookup
     * @return a {@link FileResult} containing the content and file path in case of success,
     *         or an error message in case of failure
     */
    public FileResult getFileContentOfCodeLocation(CodeLocation codeLocation, InventoryItem inventoryItem) {
        String relativePathStr = codeLocation.getFilePath();

        Path pathCheck = Paths.get(relativePathStr);
        if (pathCheck.isAbsolute() && Files.exists(pathCheck)) {
            try {
                return new FileResult.Success(Files.readString(pathCheck), pathCheck.toString());
            } catch (IOException e) {
                log.error("File exists but unreadable: {}", pathCheck, e);
                return new FileResult.Failure("Access Denied or File Locked: " + e.getMessage());
            }
        }

        String searchSuffix = relativePathStr.replace("\\", "/");

        List<File> candidates = fileRepository.findRawMatches(inventoryItem.getProject(), Path.of(relativePathStr).getFileName().toString());

        for (File candidate : candidates) {
            String absPath = candidate.getAbsolutePath();
            if (absPath.replace("\\", "/").endsWith(searchSuffix)) {
                try {
                    Path p = Paths.get(absPath);
                    if (Files.exists(p)) {
                        log.info("Resolved via DB: {}", p);
                        return new FileResult.Success(Files.readString(p), absPath);
                    }
                } catch (IOException e) {
                    log.error("Found inside DB but could not read: {}", absPath, e);
                    return new FileResult.Failure("Access Denied or File Locked: " + e.getMessage());
                }
            }
        }

        return new FileResult.Failure("File not found via Database lookup.");
    }

    /**
     * Reads the content of a file located at the given absolute path and returns
     * the result as a {@link FileResult}. This includes handling file read errors
     * or invalid paths.
     *
     * @param absolutePath the absolute path of the file to be read
     * @return a {@link FileResult} containing the file content and its path in case of success,
     *         or an error message in case of failure
     */
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
