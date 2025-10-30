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


@Service
public class FilesTreeService {

    private static final Logger log = LogManager.getLogger(FilesTreeService.class);

    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private CodeLocationRepository codeLocationRepository;

    private Project project;

    public List<FileTreeNode> prepareFilesForTreeGrid(Project project) {
        this.project = project;

        // load all necessary data
        List<CodeLocation> codeLocations = codeLocationRepository.findByInventoryItem_Project(project);
        Map<String, List<CodeLocation>> fileNameToCodeLocationsMap = new HashMap<>();
        for (CodeLocation loc : codeLocations) {
            if (loc.getFilePath() == null || loc.getFilePath().isBlank()) {
                continue;
            }
            try {
                String fileName = Paths.get(loc.getFilePath()).getFileName().toString();
                fileNameToCodeLocationsMap.computeIfAbsent(fileName, k -> new ArrayList<>()).add(loc);
            } catch (Exception e) {
                log.warn("Could not parse file path for CodeLocation with filePath: {} with error message: {}",
                        loc.getFilePath(), e.getMessage());
            }
        }

        List<InventoryItem> baseInventoryItems = inventoryItemRepository.findInventoryItemsByProjectAndParent(project
                , null);
        List<FileTreeNode> roots = new ArrayList<>();

        for (InventoryItem item : baseInventoryItems){
            if (item.getBasePath() == null) continue;
            File baseDir = new File(item.getBasePath());
            if (baseDir.exists() && baseDir.isDirectory()) {
                FileTreeNode node = buildTreeFromFile(baseDir, null, baseDir.toPath(), fileNameToCodeLocationsMap);
                roots.add(node);
            }
        }
        log.debug("Root size: {}", roots.size());
        return roots;
    }

    private FileTreeNode buildTreeFromFile(File file, FileTreeNode parent, Path basePath,
                                           Map<String, List<CodeLocation>> fileNameMap) {
        CodeLocation matchedCodeLocation = null;

        if (file.isFile()){
            String fileName = file.getName();
            String relativePath = basePath.relativize(file.toPath()).toString().replace(File.separator, "/");

            List<CodeLocation> candidates = fileNameMap.getOrDefault(fileName, Collections.emptyList());

            for (CodeLocation candidate : candidates) {
                if (candidate.getFilePath().endsWith(relativePath)) {
                    matchedCodeLocation = candidate;
                    break; // get the first matched one only
                }
            }
        }

        FileTreeNode node = new FileTreeNode(
                file.getName(),
                file.getAbsolutePath(),
                parent,
                new ArrayList<>(),
                matchedCodeLocation,
                file.isDirectory()
        );
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    FileTreeNode childNode = buildTreeFromFile(child, node, basePath, fileNameMap);
                    node.getChildren().add(childNode);
                }
            }
        }
        return node;
    }

    public String getFullPath(FileTreeNode node, String fullPath) {
        for (FileTreeNode child : node.getChildren()) {
            fullPath += getFullPath(child, fullPath);
        }
        return fullPath;
    }
}
