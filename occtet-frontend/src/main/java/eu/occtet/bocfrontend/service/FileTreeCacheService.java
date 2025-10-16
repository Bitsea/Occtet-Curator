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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.model.FileTreeNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * An application-level cache for storing generated FileTreeNode structures.
 * This prevents regenerating the tree for every user and every view load.
 */
@Service
public class FileTreeCacheService {

    private static final Logger log = LogManager.getLogger(FileTreeCacheService.class);

    @Autowired
    private FilesTreeService filesTreeService;

    private final Cache<UUID, List<FileTreeNode>> fileTreeCache = CacheBuilder.newBuilder()
            .maximumSize(100) // Store up to 100 project trees
            .expireAfterWrite(1, TimeUnit.HOURS) // Automatically evict entries after 1 hour of inactivity
            .build();

    /**
     * Gets the file tree for a project.
     * If the tree is in the cache, it's returned instantly.
     * If not, it's generated, added to the cache, and then returned.
     *
     * @param project The project for which to get the file tree.
     * @return A list of root FileTreeNode objects.
     */
    public List<FileTreeNode> getFileTree(Project project) {
        try {
            return fileTreeCache.get(project.getId(), () -> {
                return filesTreeService.prepareFilesForTreeGrid(project);
            });
        } catch (ExecutionException e) {
           log.error("Could not generate file tree for project {}, error message: {} ", project.getId(), e.getMessage());
           return new ArrayList<>();
        }
    }

    /**
     * Removes the old, stale tree from the cache.
     */
    public void invalidateCacheForProject(UUID projectId) {
        fileTreeCache.invalidate(projectId);
        log.debug("Invalidated file tree cache for project {}", projectId);
    }
}
