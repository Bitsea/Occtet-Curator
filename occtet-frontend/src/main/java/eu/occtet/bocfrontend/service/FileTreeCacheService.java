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

import eu.occtet.bocfrontend.dao.CodeLocationRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.model.FileTreeNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for building and caching file tree structures for projects.
 * Optimized for performance with batch loading and efficient path matching.
 */
@Service
public class FileTreeCacheService {

    private static final Logger log = LogManager.getLogger(FileTreeCacheService.class);

    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private CodeLocationRepository codeLocationRepository;

    private final Map<UUID, List<FileTreeNode>> fileTreeCache = new ConcurrentHashMap<>();

    /**
     * Gets the file tree for a project, using cache if available.
     */
    public List<FileTreeNode> getFileTree(Project project) {
        if (project == null) {
            return Collections.emptyList();
        }

        return fileTreeCache.computeIfAbsent(project.getId(),
                id -> buildFileTree(project));
    }

    /**
     * Clears the cache for a specific project.
     */
    public void invalidateCache(Project project) {
        if (project != null) {
            fileTreeCache.remove(project.getId());
            log.debug("Invalidated file tree cache for project: {}", project.getProjectName());
        }
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        fileTreeCache.clear();
        log.debug("Cleared entire file tree cache");
    }

    /**
     * Builds the complete file tree structure for a project.
     */
    private List<FileTreeNode> buildFileTree(Project project) {
        log.debug("Building file tree for project: {}", project.getProjectName());
        long startTime = System.currentTimeMillis();
        String basePath = project.getBasePath();
        if (basePath == null){
            log.error("No basePath in project: {}", project.getProjectName());
            return new ArrayList<>();
        }

        List<CodeLocation> codeLocations = codeLocationRepository
                .findByInventoryItem_Project(project);

        Map<String, List<CodeLocation>> fileNameIndex = indexCodeLocationsByFileName(codeLocations);

        // Build tree from base path
        List<FileTreeNode> roots = new ArrayList<>();
        FileTreeNode root = buildTreeFromBasePath(basePath, fileNameIndex);
        if(root!=null) {
            roots.add(root);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Built file tree in {}ms", duration);
        }

        return roots;
    }

    /**
     * Creates an index mapping filenames to their code locations for efficient lookup.
     */
    private Map<String, List<CodeLocation>> indexCodeLocationsByFileName(
            List<CodeLocation> codeLocations) {

        Map<String, List<CodeLocation>> index = new HashMap<>();

        for (CodeLocation location : codeLocations) {
            if (location.getFilePath() == null || location.getFilePath().isBlank()) {
                continue;
            }

            try {
                String fileName = Paths.get(location.getFilePath())
                        .getFileName()
                        .toString();

                index.computeIfAbsent(fileName, k -> new ArrayList<>())
                        .add(location);
            } catch (Exception e) {
                log.warn("Failed to parse file path '{}': {}",
                        location.getFilePath(), e.getMessage());
            }
        }

        return index;
    }

    /**
     * Builds a file tree from a base directory path.
     */
    private FileTreeNode buildTreeFromBasePath(
            String basePath,
            Map<String, List<CodeLocation>> fileNameIndex) {

        File baseDir = new File(basePath);

        if (!baseDir.exists()) {
            log.warn("Base path does not exist: {}", basePath);
            return null;
        }

        if (!baseDir.isDirectory()) {
            log.warn("Base path is not a directory: {}", basePath);
            return null;
        }

        return buildTreeNode(baseDir, null, baseDir.toPath(), fileNameIndex);
    }

    /**
     * Recursively builds a FileTreeNode from a File.
     */
    private FileTreeNode buildTreeNode(
            File file,
            FileTreeNode parent,
            Path basePath,
            Map<String, List<CodeLocation>> fileNameIndex) {

        CodeLocation codeLocation = null;

        // For files (not directories), try to find matching CodeLocation
        if (file.isFile()) {
            codeLocation = findMatchingCodeLocation(file, basePath, fileNameIndex);
        }

        FileTreeNode node = new FileTreeNode(
                file.getName(),
                file.getAbsolutePath(),
                parent,
                new ArrayList<>(),
                codeLocation,
                file.isDirectory()
        );

        // Recursively build children for directories
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    FileTreeNode childNode = buildTreeNode(
                            child, node, basePath, fileNameIndex);
                    if (childNode != null) {
                        node.getChildren().add(childNode);
                    }
                }

                // Sort children: directories first, then files, alphabetically
                node.getChildren().sort((a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
            }
        }

        return node;
    }

    /**
     * Finds the CodeLocation that matches a given file.
     */
    private CodeLocation findMatchingCodeLocation(
            File file,
            Path basePath,
            Map<String, List<CodeLocation>> fileNameIndex) {

        String fileName = file.getName();
        String relativePath = basePath.relativize(file.toPath())
                .toString()
                .replace(File.separator, "/");

        List<CodeLocation> candidates = fileNameIndex.getOrDefault(fileName,
                Collections.emptyList());

        // Find the first candidate where the relative path matches
        for (CodeLocation candidate : candidates) {
            if (candidate.getFilePath() != null &&
                    candidate.getFilePath().endsWith(relativePath)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Finds a FileTreeNode by its full path in the cached tree.
     * Returns Optional.empty() if not found or if cache is empty for the project.
     */
    public Optional<FileTreeNode> findNodeByPath(Project project, String fullPath) {
        if (project == null || fullPath == null) {
            return Optional.empty();
        }

        List<FileTreeNode> roots = fileTreeCache.get(project.getId());
        if (roots == null || roots.isEmpty()) {
            return Optional.empty();
        }

        return roots.stream()
                .map(root -> findNodeInTree(root, fullPath))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Recursively searches for a node with the given full path.
     */
    private Optional<FileTreeNode> findNodeInTree(FileTreeNode node, String fullPath) {
        if (node.getFullPath().equals(fullPath)) {
            return Optional.of(node);
        }

        for (FileTreeNode child : node.getChildren()) {
            Optional<FileTreeNode> found = findNodeInTree(child, fullPath);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }

    /**
     * Returns statistics about the cached file trees.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedProjects", fileTreeCache.size());

        int totalNodes = fileTreeCache.values().stream()
                .mapToInt(roots -> countNodesInTree(roots))
                .sum();
        stats.put("totalNodes", totalNodes);

        return stats;
    }

    private int countNodesInTree(List<FileTreeNode> roots) {
        return roots.stream()
                .mapToInt(this::countNodesRecursive)
                .sum();
    }

    private int countNodesRecursive(FileTreeNode node) {
        int count = 1; // Count this node
        for (FileTreeNode child : node.getChildren()) {
            count += countNodesRecursive(child);
        }
        return count;
    }
}
