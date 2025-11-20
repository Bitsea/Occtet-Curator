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

package eu.occtet.boc.fileindexing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for discovering files in a directory and indexing them.
 */
@Service
public class DirectoryIndexService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryIndexService.class);

    private final FileIndexService fileIndexService;

    public DirectoryIndexService(FileIndexService fileIndexService) {
        this.fileIndexService = fileIndexService;
    }

    /**
     * Indexes all files in a specified directory and its subdirectories. Each file is processed asynchronously
     * and indexed line by line. The method ensures all indexing tasks are completed before returning.
     *
     * @param basePath The base directory path to start the file indexing process.
     * @param projectId The identifier for the project under which the file indexing is to be performed.
     */
    public void indexDirectory(String basePath, String projectId) {
        Path startPath = Paths.get(basePath);
        log.info("Starting file walk for directory: {}", startPath);

        List<CompletableFuture<Void>> indexingTasks = new ArrayList<>();

        processPath(startPath, projectId, indexingTasks);

        try{
            CompletableFuture.allOf(indexingTasks.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error occurred while waiting for indexing jobs to complete: {}", e.getMessage(), e);
        }
        log.info("Finished file walk for directory: {}", startPath);
    }

    private void processPath(Path path, String projectId, List<CompletableFuture<Void>> tasks) {
        try{
            if (!Files.isReadable(path)) {
                log.warn("Skipping unreadable path: {}", path);
                return;
            }

            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        processPath(entry, projectId, tasks);
                    }
                }
            } else if (Files.isRegularFile(path)) {
                tasks.add(fileIndexService.indexFileByLines(path.toString(), projectId));
            }

        } catch (IOException e) {
            log.warn("Failed to process path {}: {}. Skipping.", path, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing path {}: {}. Skipping.", path, e.getMessage(), e);
        }
    }
}
