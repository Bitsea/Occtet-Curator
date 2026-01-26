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
import java.util.Optional;

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
     * Find the correlated file for a code location.
     */
    public Optional<File> findFileEntityForCodeLocation(CodeLocation codeLocation, InventoryItem inventoryItem) {
        // Try Direct Link (If your DownloadService set it)
        File linkedFile = fileRepository.findByCodeLocation(codeLocation);
        if (linkedFile != null) {
            return Optional.of(linkedFile);
        }

        // Try Fuzzy Path Matching
        String relativePathStr = codeLocation.getFilePath();
        if (relativePathStr == null) return Optional.empty();

        String searchSuffix = relativePathStr.replace("\\", "/");
        String fileName = Paths.get(relativePathStr).getFileName().toString();

        // Find all files with this name in the project
        List<File> candidates = fileRepository.findCandidates(
                inventoryItem.getProject(),
                fileName
        );

        for (File candidate : candidates) {
            String projectPath = candidate.getProjectPath();
            if (projectPath != null && projectPath.replace("\\", "/").endsWith(searchSuffix)) {
                return Optional.of(candidate);
            }
            String physicalPath = candidate.getPhysicalPath();
            if (physicalPath != null && physicalPath.replace("\\", "/").endsWith(searchSuffix)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Reads content using the physical path.
     *
     * @param physicalPath the absolute/physical path of the file to be read
     * @return a {@link FileResult} containing the file content and its path in case of success,
     *         or an error message in case of failure
     */
    public FileResult getFileContent(String physicalPath) {
        try {
            Path path = Paths.get(physicalPath);

            String content = Files.readString(path);
            return new FileResult.Success(content, path.toString());

        } catch (IOException e) {
            log.error("Error reading file at path '{}'", physicalPath, e);
            return new FileResult.Failure("Error reading file. Please check file permissions: " + physicalPath);
        } catch (InvalidPathException e) {
            log.error("Invalid path '{}'", physicalPath, e);
            return new FileResult.Failure("Invalid file path: " + physicalPath);
        }
    }
}
